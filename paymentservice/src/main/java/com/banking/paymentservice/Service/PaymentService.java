package com.banking.paymentservice.Service;

import com.banking.paymentservice.DTO.CreatePaymentRequest;
import com.banking.paymentservice.DTO.PaymentOrderResponse;
import com.banking.paymentservice.Entity.Payment;
import com.banking.paymentservice.Entity.PaymentStatus;
import com.banking.paymentservice.Repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    //Topic variables
    private static  final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static  final String PAYMENT_FAILED_TOPIC = "payment.failed";

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    /**
     * Create RazorPay  payment order
     * Flow
     * 1. Create order in razor pay
     * 2. Save the payment
     * 3. Return order details to frontend
     * 4. Front end shows RazorPay check Out page
     * 5. User pays
     * 6.RazorPay calls webhook(Success/Failure)
     * @param request
     * @return
     */
    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest request) throws RazorpayException {
        log.info("Creating payment order for account:{} amount:{}",request.getAccountNumber(),request.getAmount());

        RazorpayClient razorpayClient = new RazorpayClient(keyId,keySecret);//keyid , key secrets , payment gateway

        //convert currency into smaller amount(USD -> cents Eg:1 USD = 100cents,1INR =100 paise)
        int convertedAmount = request.getAmount()
                .multiply(BigDecimal.valueOf(100))//Multiple into 100 to get smaller amount
                .intValue();//BigDecimal -> int

        JSONObject orderRequest = new JSONObject();//store data in Key-Value pair
        orderRequest.put("amount",convertedAmount);
        orderRequest.put("currency","USD/INR");
        orderRequest.put("receipt","rcpt_"+ System.currentTimeMillis()+UUID.randomUUID().toString()
                .replace("-","").substring( 0, 10));//Generate same UUID
       Order razorpayOrder = razorpayClient.orders.create(orderRequest);

       log.info("Razorpay order created:{}",razorpayOrder.get("id").toString());

       //Save the payment
        Payment payment = new Payment();
        payment.setRazorPayOrderId(razorpayOrder.get("id").toString());
        payment.setAccountNumber(request.getAccountNumber());
        payment.setCurrency("USD/INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(request.getDescription());

        Payment savedPayment = paymentRepo.save(payment);
        return new PaymentOrderResponse(
                savedPayment.getId(),
                razorpayOrder.get("id").toString(),
                request.getAmount(),"USD/INR",
                "CREATED",
                keyId);
    }

    public void handleWebHook(Map<String, Object> payload) {
         log.info("Received Razorpay Webhook:{}",payload.get("event"));
         String event =(String) payload.get("event");
         
         //if payment succeeds
         if("payment.captured".equals(event)){
             handlePaymentSuccess(payload);
         }
         //if payment fails
         else if ("payment.failed".equals(event)){
             handlePaymentFailure(payload);
         }

    }

    private void handlePaymentFailure(Map<String, Object> payload) {
        try{
         // extract data from payload
            Map<String,Object> paymentData = extractPaymentData(payload);
            String orderId = (String) paymentData.get("orderId");
            String paymentId =(String) paymentData.get("paymentId");

            //get payment from OrderId
            Payment payment = paymentRepo.findByRazorpayOrderId(orderId);
        }
        catch (Exception e){

        }
    }

    private void handlePaymentSuccess(Map<String, Object> payload) {
    }
}
