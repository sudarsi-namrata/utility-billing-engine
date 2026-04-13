package com.utility.billing.controller;

import com.utility.billing.model.Payment;
import com.utility.billing.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody PaymentDto dto) {
        Payment payment = paymentService.processPayment(dto.accountNumber, dto.amount, dto.method);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<List<Payment>> getPayments(@PathVariable String accountNumber) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(accountNumber));
    }

    @PostMapping("/{paymentRef}/reverse")
    public ResponseEntity<Payment> reversePayment(@PathVariable String paymentRef) {
        return ResponseEntity.ok(paymentService.reversePayment(paymentRef));
    }

    public record PaymentDto(
            String accountNumber,
            BigDecimal amount,
            Payment.PaymentMethod method
    ) {}
}
