package com.banking.paymentservice.DTO;

import com.banking.paymentservice.Entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentOrderResponse {

    private  String paymentId;
    private String razorPayOrderId;
    private BigDecimal amount;
    private String currency;
    private String status;//Dont include Enum"PaymentStatus"
    private String razorPayKeyId;




}
