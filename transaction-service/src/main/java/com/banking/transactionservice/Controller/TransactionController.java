package com.banking.transactionservice.Controller;

import com.banking.transactionservice.DTO.TransactionResponse;
import com.banking.transactionservice.DTO.TransferRequest;
import com.banking.transactionservice.Service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@Slf4j
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    //Create Transfer
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse>transfer(@Valid @RequestBody TransferRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request));
    }

    //Get a Transaction by transactionId
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse>getTransaction(@PathVariable String transactionId){
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }
    //Get a Transaction History by account Number -> gives list of transactions
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>>getTransactionHistory(@PathVariable String accountNumber){
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber));
    }

    //Verify the OTP with TransactionId
    @PostMapping("/{transactionId}/verify")
    public ResponseEntity<TransactionResponse> verifyOTP(@PathVariable String transactionId,@RequestParam String OTP){
        log.info("OTP verification request-transaction:{}",transactionId);
        return ResponseEntity.ok(transactionService.verifyOTP(transactionId,OTP));
    }



}
