package com.banking.accountservice.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;
/*
Consume Transaction completed event from kafka
Credit  Receives amount if no fraud is detected
 @Param payload
 */
  @KafkaListener(topics ="transaction.completed")
    public void consumeTransactionCompleted(@Payload Map<String,Object> payload){
        //@Payload- Bind body of incoming message to method parameter in kafka
     try{
       String receiverAccount = (String)payload.get("receiverAccountNumber");
       BigDecimal amount = new BigDecimal(payload.get("amount").toString());

       log.info("Crediting account:{} amount:{}",receiverAccount,amount);

       accountService.creditBalance(receiverAccount,amount);

      }
      catch(Exception e){
         log.error("Error Crediting Account:{}",e.getMessage());

      }
    }
/**
 * Consume event from Kafka
 * Blocks the flagged account
 */
     @KafkaListener(topics="fraud.Detected")
    public void fraudDetected(@Payload Map<String,Object> payload){
      try {
          String accountNumber = (String) payload.get("accountNumber");
          log.info("Fraud Detected -Blocking account:{}", accountNumber);
          accountService.blockAccount(accountNumber);
      }
      catch (Exception e){
          log.info("Error blocking account:{}",e.getMessage());
      }

    }


}
