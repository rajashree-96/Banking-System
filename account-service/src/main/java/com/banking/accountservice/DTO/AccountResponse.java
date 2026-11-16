package com.banking.accountservice.DTO;

import com.banking.accountservice.Entity.AccountStatus;
import com.banking.accountservice.Entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {

    private String Id;
    private String accountNumber;
    private String accountHolderNumber;
    private String email;
    private String phone;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private BigDecimal dailyTransactionLimit;
    private LocalDateTime createdAt;

}
