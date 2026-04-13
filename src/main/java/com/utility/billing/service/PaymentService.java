package com.utility.billing.service;

import com.utility.billing.model.*;
import com.utility.billing.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepo;
    private final BillRepository billRepo;
    private final AccountRepository accountRepo;

    public PaymentService(PaymentRepository paymentRepo, BillRepository billRepo,
                          AccountRepository accountRepo) {
        this.paymentRepo = paymentRepo;
        this.billRepo = billRepo;
        this.accountRepo = accountRepo;
    }

    /**
     * Process a payment for an account. Allocates to oldest unpaid bills first (FIFO).
     * Supports partial payments — leftover stays as credit on account.
     */
    @Transactional
    public Payment processPayment(String accountNumber, BigDecimal amount, Payment.PaymentMethod method) {
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        String paymentRef = "PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        Payment payment = new Payment(paymentRef, account, amount, method);

        // Allocate payment to oldest unpaid bills (FIFO)
        List<Bill> unpaidBills = billRepo.findUnpaidBills(account.getId());
        BigDecimal remaining = amount;

        for (Bill bill : unpaidBills) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal billDue = bill.getBalanceDue();
            BigDecimal allocated = remaining.min(billDue);

            bill.applyPayment(allocated);
            billRepo.save(bill);

            remaining = remaining.subtract(allocated);

            if (payment.getBill() == null) {
                payment.setBill(bill); // link to first bill for reference
            }

            log.debug("Applied ${} to bill {}, remaining due: ${}",
                    allocated, bill.getBillNumber(), bill.getBalanceDue());
        }

        // Update account balance
        account.setBalance(account.getBalance().subtract(amount));
        accountRepo.save(account);

        payment.markPosted();
        paymentRepo.save(payment);

        log.info("Payment {} posted for account {} - ${} via {}",
                paymentRef, accountNumber, amount, method);
        return payment;
    }

    public List<Payment> getPaymentHistory(String accountNumber) {
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
        return paymentRepo.findByAccountIdOrderByTransactionDateDesc(account.getId());
    }

    /**
     * Reverse a payment (e.g., returned check).
     */
    @Transactional
    public Payment reversePayment(String paymentRef) {
        Payment payment = paymentRepo.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentRef));

        if (payment.getStatus() != Payment.PaymentStatus.POSTED) {
            throw new IllegalStateException("Can only reverse posted payments");
        }

        payment.setStatus(Payment.PaymentStatus.REVERSED);
        paymentRepo.save(payment);

        // Re-add balance to account
        Account account = payment.getAccount();
        account.setBalance(account.getBalance().add(payment.getAmount()));
        accountRepo.save(account);

        // TODO: re-open affected bills if they were marked PAID

        log.warn("Payment {} reversed for account {} - ${}",
                paymentRef, account.getAccountNumber(), payment.getAmount());
        return payment;
    }
}
