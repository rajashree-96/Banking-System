package com.banking.transactionservice.Repository;

import com.banking.transactionservice.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction,String> {

    List<Transaction> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);

}
