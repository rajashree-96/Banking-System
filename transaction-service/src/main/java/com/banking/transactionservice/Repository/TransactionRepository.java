package com.banking.transactionservice.Repository;

import com.banking.transactionservice.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction,String> {
}
