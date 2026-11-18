package com.banking.accountservice.Service;

import com.banking.accountservice.DTO.AccountResponse;
import com.banking.accountservice.DTO.CreateAccountRequest;
import com.banking.accountservice.Entity.Account;
import com.banking.accountservice.Entity.AccountStatus;
import com.banking.accountservice.Entity.AccountType;
import com.banking.accountservice.Repository.AccountRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private AccountRepo accountRepo;
    private static  SecureRandom secureRandom = new SecureRandom();//to generate random account number

    //set values in Account from user posted (PostMapping)
    public AccountResponse createAccount(CreateAccountRequest request){
        log.info("Creating Account for {}",request.getEmail());
        if(accountRepo.existsByEmail(request.getEmail())){//write method in accountRepo to check mail
            throw new RuntimeException("Account already exist for email: "+request.getEmail());
        }
        Account account= new Account();
        account.setAccountHolderNumber(request.getAccountHolderNumber());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);//account created -> ACTIVE
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber());
        //generate account number which should be unique and 12 digits
      account.setDailyTransactionLimit(
 // transaction limit -> depends on account type
        request.getAccountType()== AccountType.SAVINGS
                ? new BigDecimal("100000")//if savings limit -> 100,000
                : new BigDecimal(500000));//if Current/FD -> 500,000

        Account savedAccount = accountRepo.save(account);
        log.info("Account created :{}"+savedAccount.getAccountNumber());
        return mapToReponse(savedAccount);

    }

    //get accountNumber and check if it is already present or not .Throw exception if not found
    public AccountResponse getAccount(String accountNumber){
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));
        return mapToReponse(account);
    }

    //get Balance by accountNumber
    public BigDecimal getBalance(String accountNumber){
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));
        return account.getBalance();
    }

   /*
   Block account - called by Fraud detection service via Kafka
   @param accountNumber  */
    public void blockAccount(String accountNumber){
       log.info("Blocking Account:{}",accountNumber);
       Account account= accountRepo.findByAccountNumber(accountNumber)
               .orElseThrow(()->new RuntimeException("Account not found"));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepo.save(account);
        log.info("Account blocked:{}",accountNumber);
    }

    /* Deduct Balance -called by Transaction service
    @param accountNumber , @param  amount  */
    public void deductBalance(String accountNumber,BigDecimal amount){
        log.info("Deducting balance {} from account{}",amount,accountNumber);

        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));
        if (account.getStatus()!=AccountStatus.ACTIVE){
            throw new RuntimeException("Account is not active:"+accountNumber);
        }
        if(account.getBalance().compareTo(amount)<0){
            throw new RuntimeException(("Insufficient fund for account"+accountNumber));
        }
        account.setBalance(account.getBalance().subtract(amount));//balance = balance - amount
        accountRepo.save(account);
        log.info("Balance Updated.New Balance :{} ",account.getBalance());
    }


    /* Credit Balance -called by Transaction service via Kafka
        @param accountNumber , @param  amount  */
    public void creditBalance(String accountNumber,BigDecimal amount){
        log.info("Crediting {} to account{}",amount,accountNumber);
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));
        account.setBalance(account.getBalance().add(amount));//adding amount to balance
        accountRepo.save(account);
        log.info("Balance credited. New Balance:{}",account.getBalance());

    }

    //generate unique 12 digit account number
    private String generateAccountNumber() {
        String accountNumber;
        do{
          long number  = secureRandom.nextLong(1_000_000_000_000L);//set bound t0 12 digits
          accountNumber = String.format("%012d");//if number is fewer than 12 ,pad left side with leading zeros
        }while(accountRepo.existsByAccountNumber(accountNumber));//if number exists ,then generate other random
       return accountNumber;
    }

    //set values from Account (Entity) to AccountResponse(DTO) -->To store values in DTO when account is created
    private AccountResponse mapToReponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountHolderNumber(account.getAccountHolderNumber());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAccountType(account.getAccountType());
        response.setAccountType(account.getAccountType());
        response.setStatus(account.getStatus());
        response.setBalance(account.getBalance());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }


}
