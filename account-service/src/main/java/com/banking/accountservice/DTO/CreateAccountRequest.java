package com.banking.accountservice.DTO;

import com.banking.accountservice.Entity.AccountType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
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
public class CreateAccountRequest {

    @NotBlank(message = "Account Holder is required")
    private String accountHolderNumber;

    @NotBlank(message ="Email is required")
    @Email(message = "Invalid Email format")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Initial Deposit is required")
    @Positive(message = "Initial Deposit must be positive")
    private BigDecimal initialDeposit;

}
