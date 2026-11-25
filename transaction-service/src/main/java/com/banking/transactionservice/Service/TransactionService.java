package com.banking.transactionservice.Service;


import com.banking.transactionservice.Client.AccountServiceClient;
import com.banking.transactionservice.DTO.TransactionResponse;
import com.banking.transactionservice.DTO.TransferRequest;
import com.banking.transactionservice.Entity.Transaction;
import com.banking.transactionservice.Entity.TransactionStatus;
import com.banking.transactionservice.Entity.TransactionType;
import com.banking.transactionservice.Event.TransactionCompletedEvent;
import com.banking.transactionservice.Event.TransactionInitiatedEvent;
import com.banking.transactionservice.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private  final TransactionRepository transactionRepo;
    private final AccountServiceClient accountServiceClient;
    //Kafka Template
    private final KafkaTemplate<String,Object> kafkaTemplate;
    //Redis template
    private final RedisTemplate<String,String>redisTemplate;

    //kafka topics variables
    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String TRANSACTION_FRAUD_DETECTED_TOPIC = "fraud.detected";
    /**
     * SAGA Step 1:Initiate Transfer
     * Deducts from sender called from AccountService via FeignClient(AccountServiceClient)
     * Saves transaction as PROCESSING
     * Publish event to Kafka for fraud Check
     * Returns.
     * @param request
     * @return
     */
   public TransactionResponse transfer(TransferRequest request){
       log.info("SAGA Start -Transfer: {} ->{} amount: {}" ,
               request.getSenderAccountNumber(),
               request.getReceiverAccountNumber(),
               request.getAmount());
       //SAGA Step1: Deduct from Sender
       accountServiceClient.deductBalance(
               request.getSenderAccountNumber(),
               request.getAmount());

       //Saves transaction as PROCESSING
       Transaction transaction =new Transaction();
       transaction.setSenderAccountNumber(request.getSenderAccountNumber());
       transaction.setReceiverAccountNumber((request.getReceiverAccountNumber()));
       transaction.setAmount(request.getAmount());
       transaction.setType(TransactionType.TRANSFER);//set transaction type as TRANSFER
       transaction.setStatus(TransactionStatus.PROCESSING);
       transaction.setDescription(request.getDescription());
       transaction.setReferenceNumber(UUID.randomUUID().toString());

       Transaction savedTransaction = transactionRepo.save(transaction);
       log.info("Transaction saved as PROCESSING :{}",savedTransaction.getId());

       //SAGA 2: Publish event to Kafka for fraud Check
       TransactionInitiatedEvent event =new TransactionInitiatedEvent(
               savedTransaction.getId(),
               savedTransaction.getSenderAccountNumber(),
               savedTransaction.getReceiverAccountNumber(),
               savedTransaction.getAmount(),
               savedTransaction.getDescription()
       );

       kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC ,savedTransaction.getId(),event);//Transaction Initiated
       log.info("SAGA -Step2 -TransactionInitiation Event published :{}",savedTransaction.getId());

      return mapToResponse(savedTransaction);//Have to return TransactionResponse but it is in Transaction

   }

   public TransactionResponse getTransaction(String transactionId){
       return mapToResponse(transactionRepo
               .findById(transactionId)
               .orElseThrow(()-> new RuntimeException("Transaction not found:"+transactionId)));
   }

    public List<TransactionResponse> getTransactionHistory(String accountNumber){
//Create query method
        return transactionRepo.findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)//Transaction to TransactionResponse
                .collect(Collectors.toList());
    }

    public TransactionResponse verifyOTP(String transactionId,String otp){
       log.info("OTP verification for the transaction :{}",transactionId);
       Transaction transaction = transactionRepo.findById(transactionId)
               .orElseThrow(()-> new RuntimeException("Transaction not found "+transactionId));
       //OTP must match with otp generated in TransactionEventConsumer(Service)
        String otpKey = "Verification:otp" +transactionId;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if(storedOtp == null){//otp is expired
            log.warn("OTP expired for transaction:{} ",transactionId);
            compensateTransaction(transaction,"OTP expired -transaction cancelled and amount refunded");
            return mapToResponse(transaction);
        }

        if(!storedOtp.equals(otp)){//storedOtp(Redis) and otp(given by user) are not matching
            //Block account and amount to be refunded
            log.warn("Wrong OTP- blocking account and refunding:{}",transactionId);
            redisTemplate.delete(otpKey);//delete the transaction
            blockAccountAndCompensate(transaction,"Wrong OTP entered -transaction cancelled"+
                    "account blocked for security");
            return mapToResponse(transaction);
        }
        //OTP correct - complete transaction
        log.info("OTP verified -completing transaction:{}",transactionId);
        redisTemplate.delete(otpKey);
        completeTransaction(transaction);
        return mapToResponse(transaction);

    }

    private void compensateTransaction(Transaction transaction, String reason) {
       log.warn("SAGA COMPENSATION -refunding :{} amount:{}",
               transaction.getSenderAccountNumber(),transaction.getAmount());

       //Credit money back to sender Synchronously - done by Feign Client(AccountServiceClient)
        accountServiceClient.creditBalance(transaction.getSenderAccountNumber(), transaction.getAmount());

        transaction.setStatus(TransactionStatus.FLAGGED);//it is blocked
        transaction.setFailureReason(reason+" - SAGA Compensation executed ,amount refunded"+ LocalDateTime.now());

        transactionRepo.save(transaction);

        //Publish Refund Event - Notification Service will alert user (refund is issued)
        Map<String,Object> refundEvent = new HashMap<>();
        refundEvent.put("transactionId",transaction.getId());
        refundEvent.put("senderAccountNumber",transaction.getSenderAccountNumber());
        refundEvent.put("amount",transaction.getAmount());
        refundEvent.put("reason",reason);

       kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC,transaction.getId(),refundEvent);
       log.info("SAGA COMPENSATION COMPLETE-{} refunded to {}", transaction.getAmount(),transaction.getSenderAccountNumber());

    }

    private void blockAccountAndCompensate(Transaction transaction, String reason) {
       //Publish fraud.detected event  -> Account Service will block account
        Map<String,Object> fraudEvent = new HashMap<>();
        fraudEvent.put("transactionId",transaction.getId());
        fraudEvent.put("accountNumber",transaction.getSenderAccountNumber());
        fraudEvent.put("reason",reason);

        kafkaTemplate.send(TRANSACTION_FRAUD_DETECTED_TOPIC,transaction.getSenderAccountNumber(),fraudEvent);
        log.warn("fraud.detected published:account{} will be blocked,Kindly contact to your bank",transaction.getSenderAccountNumber());

      //SAGA COMPENSATION -refund sender
        compensateTransaction(transaction,reason);
    }

    private void completeTransaction(Transaction transaction) {
           transaction.setStatus(TransactionStatus.COMPLETED);
           transaction.setCompletedAt(LocalDateTime.now());
           transactionRepo.save(transaction);
           //Publish event
        TransactionCompletedEvent completedEvent= new TransactionCompletedEvent(
                                        transaction.getId(), transaction.getSenderAccountNumber(),
                 transaction.getReceiverAccountNumber(),transaction.getAmount(),transaction.getDescription());
        kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC,transaction.getId(),completedEvent);
        log.info("SAGA COMPLETE -Transaction:{}completed",transaction.getId());
    }

    public void processCleanResult(String transactionId) {
        Transaction transaction = transactionRepo.findById(transactionId)
                .orElseThrow(()-> new RuntimeException("Transaction not found "+transactionId));

        //verification required Event
        if(transaction.getStatus()!= TransactionStatus.PROCESSING){//if completed/blocked ,skip transaction 2nd time
            log.warn("Transaction{} not PROCESSING -skipping",transactionId);
            return ;
        }
        completeTransaction(transaction);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
//convert Transaction to TransactionResponse
       TransactionResponse transactionResponse =new TransactionResponse();
        transactionResponse.setId(transaction.getId());
        transactionResponse.setSenderAccountNumber(transaction.getSenderAccountNumber());
        transactionResponse.setReceiverAccountNumber(transactionResponse.getReceiverAccountNumber());
        transactionResponse.setAmount(transaction.getAmount());
        transactionResponse.setDescription(transaction.getDescription());
        transactionResponse.setReferenceNumber(transaction.getReferenceNumber());
        transactionResponse.setFailureReason(transaction.getFailureReason());
        transactionResponse.setCreatedAt(transaction.getCreatedAt());
        transactionResponse.setCompletedAt(transaction.getCompletedAt());
        return transactionResponse;
    }



}
