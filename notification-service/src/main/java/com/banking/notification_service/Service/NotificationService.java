package com.banking.notification_service.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(@Payload Map<String,Object> payload){
        try{
            String accountNumber =(String) payload.get("accountNumber");
            String otp =(String) payload.get("otp");
            String transactionId = (String) payload.get("transactionId");
            String amount =(String) payload.get("amount");
            String reason = payload.get("reason").toString();

            sendAlert(accountNumber,
                    "TRANSACTION VERIFICATION REQUIRED",
                    String.format("Suspicious Activity on your account" +
                            "Reason: %s"+
                            "A transaction of %s is pending verification"+
                            "your OTP is %s and valid for 5 mins"+
                            "If this wasn't you -ignore this message"));

        }
        catch(Exception e){
            log.error("Error sending OTP notification:{}",e.getMessage());

        }
    }
//To send alerts (fraud,suspicious )
    private void sendAlert(String accountNumber,String subject, String message) {


    }

}
