package com.banking.transactionservice.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCompletedEvent {

    private String transactionId;
    private String SenderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private String description;

}
