package com.banking.accountservice.Controller;

import com.banking.accountservice.DTO.AccountResponse;
import com.banking.accountservice.DTO.CreateAccountRequest;
import com.banking.accountservice.Entity.Account;
import com.banking.accountservice.Repository.AccountRepo;
import com.banking.accountservice.Service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
//Generates constructor for uninitialised final fields and  fields marked with @non null and no use of @Autowiring
public class AccountController {

    private final AccountService accountService;

    /*  Create Account,Get Account,Get balance,Block Account*/
   @PostMapping
   public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
//@valid - must meet constraints @notNull,@notBlanketc  @RequestBody - converts JSON into Java Object
       return ResponseEntity.status(HttpStatus.CREATED)
               .body(accountService.createAccount(request));
   }

   @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber){
        //@PathVariable -extract values from the URI path ( unique resource identifiers (/foos/{id}).)
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String>blockAccount(@PathVariable String accountNumber){
       accountService.blockAccount(accountNumber);
       return ResponseEntity.ok("Account blocked successfully");
    }
    /* Deduct balance- SAGA(Step -1) ,
    Called by Transaction service when transfer is initiated*/

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String>deductBalance
            (@PathVariable String accountNumber,@RequestParam BigDecimal amount){
        //@RequestParam -extract the values from query
           accountService.deductBalance(accountNumber,amount);
           return ResponseEntity.ok("Balance deducted successfully");
    }
    /* SAGA STEP 4 -Compensation service endpoint
      Called by transaction service in two scenarios
      1.Fraud detected -> refund sender(undo step 1)
      2. Transaction completed -> Credit receiver
     Credit Balance(SAGA step -4)--> Credit Sender,Credit Balance */

   @PutMapping("{accountNumber}/credit")
    public ResponseEntity<String> creditBalance
            (@PathVariable String accountNumber,@RequestParam BigDecimal amount){
        accountService.creditBalance(accountNumber,amount);
        return ResponseEntity.ok("Balance credited successfully");
    }




}
