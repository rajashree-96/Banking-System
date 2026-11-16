package com.banking.accountservice.Repository;

import com.banking.accountservice.Entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRepo extends JpaRepository<Account,String> {

    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);
}
