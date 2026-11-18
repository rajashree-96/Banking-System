package com.banking.fraud_detection_service.Service;

import com.banking.fraud_detection_service.Client.AccountServiceClient;
import com.banking.fraud_detection_service.Model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    //topic variable
    private final String VERIFICATION_REQUIRED_TOPIC ="verification.required";
    private final String FRAUD_CHECK_CLEAN_RESULT_TOPIC ="fraud.check.clean";
    private final KafkaTemplate <String,Object> kafkaTemplate;

    public void checkTransaction(Map<String, Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String)payload.get("senderAccountNumber");
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        //Fetch real Balance from Account Service(getBalance()-> provides balance) via AccountServiceClient
        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);

        log.info("Checking transaction:{} account:{} amount:{} balance:{}",
                transactionId,accountNumber,amount,senderBalance);
        FraudCheckResult result= performFraudChecks(accountNumber,amount,senderBalance);

        if(result.isFraud()) {
            log.info("Suspicious activity detected -account:{}" +
                    "reason:{}-requesting OTP verification", accountNumber, result.getReason());

            Map<String, Object> verificationEvent = new HashMap<>();
            verificationEvent.put("transactionId", transactionId);
            verificationEvent.put("accountNumber", accountNumber);
            verificationEvent.put("amount", amount);
            verificationEvent.put("reason", result.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC, transactionId, verificationEvent);
        }
        else{//No fraud is detected
            log.info("Transaction is clean");
            Map<String, Object> transactionCleanEvent = new HashMap<>();
            transactionCleanEvent.put("transactionId",transactionId);
            transactionCleanEvent.put("isFraud",false);//no fraud
            transactionCleanEvent.put("reason",null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC,transactionId,transactionCleanEvent);
            
        }
    }

    private FraudCheckResult performFraudChecks(String accountNumber, BigDecimal amount, BigDecimal senderBalance) {


    }


    
}
