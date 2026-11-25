package com.banking.paymentservice.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message="Amount mustn't be null")
    @Positive(message = "Amount must be Positive")
    private BigDecimal amount;

    private String description;




}
