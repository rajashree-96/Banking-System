package com.banking.transactionservice.DTO;

import com.banking.transactionservice.Entity.TransactionStatus;
import com.banking.transactionservice.Entity.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {

    private  String id;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private String description;
    private TransactionType type;
    private TransactionStatus status;
    private String failureReason;
    private String referenceNumber;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
