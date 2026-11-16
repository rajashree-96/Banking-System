package com.banking.accountservice.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="Accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String Id;

    @Column(nullable = false ,unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolderNumber;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(nullable = false,precision = 15,scale = 2)
    private BigDecimal balance;

    @Column(nullable = false,precision = 15,scale =2)
    private BigDecimal dailyTransactionLimit;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
