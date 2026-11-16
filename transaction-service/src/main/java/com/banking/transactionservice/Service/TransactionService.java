package com.banking.transactionservice.Service;


import com.banking.transactionservice.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private  final TransactionRepository transactionRepo;

}
