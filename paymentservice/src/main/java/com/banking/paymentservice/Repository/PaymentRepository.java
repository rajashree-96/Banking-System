package com.banking.paymentservice.Repository;

import com.banking.paymentservice.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PaymentRepository extends JpaRepository<Payment,String> {
}
