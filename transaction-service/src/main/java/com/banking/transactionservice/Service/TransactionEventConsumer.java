package com.banking.transactionservice.Service;

import com.banking.transactionservice.Entity.Transaction;
import com.banking.transactionservice.Entity.TransactionStatus;
import com.banking.transactionservice.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
    /**
     * Consume VERIFICATION_REQUIRED_TOPIC (FraudDetectionService)
     * Generate OTP and ask user to verify
     * @param payload
     */
    private static final long OTP_EXPIRE_MINUTES = 5;
    private final TransactionRepository transactionRepo;
    private final TransactionService transactionService;
    //Kafka and Redis Template
    private final KafkaTemplate<String,Object>kafkaTemplate;
    private final RedisTemplate<String,String> redisTemplate;
    //topic
    private static  final String TRANSACTION_OTP_GENERATED_TOPIC ="transaction.otp.generated";

    public void consumeVerificationRequired(@Payload Map<String,Object> payload){
     try{
          String transactionId = (String) payload.get("transactionId");
          String accountNumber = (String) payload.get("accountNumber");
          String reason = (String) payload.get("reason");
          log.info("Verification Required - transaction:{} reason:{}",
                  transactionId,reason);
         Transaction transaction = transactionRepo.findById(transactionId)
                 .orElseThrow(()-> new RuntimeException("Transaction not found "+transactionId));

         if(transaction.getStatus()!= TransactionStatus.PROCESSING){//if completed/blocked ,skip transaction 2nd time
             log.warn("Transaction{} not PROCESSING -skipping",transactionId);
             return ;
         }
         //Generate 6 digit OTP
         String OTP = String.format("%06d",(int)(Math.random()*900000)+100000);

         //Store OTP in redis  -expires in 5 mins
         String OtpKey= "Verification:otp" +transactionId;
         redisTemplate.opsForValue().set(OtpKey,OTP, OTP_EXPIRE_MINUTES, TimeUnit.MINUTES);

         //Update status
         transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
         transactionRepo.save(transaction);
         log.info("OTP generated for transaction:{} expires in {} mins ",transactionId,OTP_EXPIRE_MINUTES);

         //Notify user after OTP is generated
         Map<String,Object > otpEvent = new HashMap<>();
         otpEvent.put("transactionId",transactionId);
         otpEvent.put("accountNumber",accountNumber);
         otpEvent.put("reason",reason);
         otpEvent.put("otp",OTP);
         otpEvent.put("amount",payload.get("amount"));//get from kafka

         kafkaTemplate.send("TRANSACTION_OTP_GENERATED_TOPIC",transactionId,otpEvent);
        // TRANSACTION_OTP_GENERATED_TOPIC -(event)> consumed by notificationService
     }
     catch(Exception e){
       log.error("Error handling Verification required :{}",e.getMessage());
     }
    }

    @KafkaListener(topics = "fraud.check.clean")
    public void  consumeFraudCheckCleanResult(@Payload Map<String,Object> payload){
          try{
              String transactionId= (String) payload.get("transactionId");
              String accountNumber = (String) payload.get("accountNumber");
              transactionService.processCleanResult(transactionId);

          }
          catch(Exception e){
            log.error("Error Processing Fraud check :{}",e.getMessage());
          }
    }
}
