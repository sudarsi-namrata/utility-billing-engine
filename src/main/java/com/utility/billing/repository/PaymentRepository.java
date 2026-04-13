package com.utility.billing.repository;

import com.utility.billing.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByAccountIdOrderByTransactionDateDesc(Long accountId);

    Optional<Payment> findByPaymentRef(String paymentRef);

    List<Payment> findByStatus(Payment.PaymentStatus status);
}
