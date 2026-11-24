package com.banking.fraud_detection_service.Service;
import org.springframework.beans.factory.annotation.Value;
import com.banking.fraud_detection_service.Client.AccountServiceClient;
import com.banking.fraud_detection_service.Model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    //topic variable
    private final String VERIFICATION_REQUIRED_TOPIC ="verification.required";
    private final String FRAUD_CHECK_CLEAN_RESULT_TOPIC ="fraud.check.clean";

    @Value("${fraud.max-transactions-per-minute}")//import org.springframework.beans.factory.annotation.Value;
    private int maxTransactionsPerMinute;
    @Value("${fraud.suspicious-amount-multiplier}")
    private double suspiciousMultiplier;//multiplier= 5
    @Value("${fraud:max-balance-percentage}")
    private double maxBalancedPercentage; //0.90(90%)

    //Kafka Template
    private final KafkaTemplate <String,Object> kafkaTemplate;

    //Redis template
    private final RedisTemplate<String,String> redisTemplate;

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
        else{//No fraud is detected or Transaction is Clean
            log.info("Transaction is clean");
            Map<String, Object> transactionCleanEvent = new HashMap<>();
            transactionCleanEvent.put("transactionId",transactionId);
            transactionCleanEvent.put("isFraud",false);//no fraud
            transactionCleanEvent.put("reason",null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC,transactionId,transactionCleanEvent);
            
        }
    }

    private FraudCheckResult performFraudChecks(String accountNumber, BigDecimal amount, BigDecimal senderBalance){

        //Pattern 1: Velocity Check
        if(isVelocityExceeded(accountNumber)){
            return new FraudCheckResult(true,"Too many transactions in 60 seconds "+
                    "- Velocity limit exceeded");
        }
       //Pattern 2: Amount check
       if(isAmountSuspicious(accountNumber,amount)){
           return new FraudCheckResult(true,"Unusual transaction amount "+
                   "- exceeds 3X your average ");
       }
       //Pattern 3: Balance Check
        if(senderBalance.compareTo(BigDecimal.ZERO) >0 && isBalanceCheckFailed(senderBalance,amount)){
            return new FraudCheckResult(true,"Transaction exceeds 90% of account balance ");
        }

        return new FraudCheckResult(false,null);//When the transaction is clean
    }


    private boolean isVelocityExceeded(String accountNumber) {
        String key = "fraud-velocity "+accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);

        if(count!=null && count ==1){
            redisTemplate.expire(key,60, TimeUnit.SECONDS);//expires in 60 seconds
        }
        log.info("Velocity check - account: {} count:{}/{}",accountNumber,count,maxTransactionsPerMinute);
        return count!= null && count>maxTransactionsPerMinute; //if count is non null and greater than 5
    }


    private boolean isAmountSuspicious(String accountNumber, BigDecimal amount) {
        String avgKey = "fraud:avg_amount" +accountNumber;
        String avgStr = redisTemplate.opsForValue().get(avgKey);

        if(avgStr==null){//if this is first time transaction,it stores in redis
            redisTemplate.opsForValue().set(avgKey,amount.toString());//saves avgKey with amount in redis
            return false;
        }
        //Calculate Fraud threshold
        BigDecimal avgAmount = new BigDecimal(avgStr);
        BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(suspiciousMultiplier));//limit to which avg transaction shouldn't exceed

        //update running new Average
        BigDecimal newAvg = avgAmount.add(amount).divide(BigDecimal.valueOf(2),2, RoundingMode.HALF_UP);
        redisTemplate.opsForValue().set(avgKey,newAvg.toString());

        log.info("Amount check -amount:{} -threshold:{} suspicious:{}",
                amount,threshold,amount.compareTo(threshold)>0);
        return amount.compareTo(threshold)>0;//if amount > threshold -> true
    }


    private boolean isBalanceCheckFailed(BigDecimal senderBalance, BigDecimal amount) {
        BigDecimal maxAllowed =senderBalance.multiply(
                            BigDecimal.valueOf(maxBalancedPercentage));
        log.info("Balance check -amount:{} maxAllowed:{} suspicious:{}",
                 amount,maxAllowed,amount.compareTo(maxAllowed)>0);
        return amount.compareTo(maxAllowed)>0;//amount>maxAllowed -> true

    }

}
