package com.banking.transactionservice.Service;


import com.banking.transactionservice.Client.AccountServiceClient;
import com.banking.transactionservice.DTO.TransactionResponse;
import com.banking.transactionservice.DTO.TransferRequest;
import com.banking.transactionservice.Entity.Transaction;
import com.banking.transactionservice.Entity.TransactionStatus;
import com.banking.transactionservice.Entity.TransactionType;
import com.banking.transactionservice.Event.TransactionInitiatedEvent;
import com.banking.transactionservice.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
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

    //kafka topics variables
    private static final String TRANSACTION_INITIATED_TOPIC ="transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC ="transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC ="transaction.refunded";

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
