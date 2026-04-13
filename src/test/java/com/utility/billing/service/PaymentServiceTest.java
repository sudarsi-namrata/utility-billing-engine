package com.utility.billing.service;

import com.utility.billing.model.*;
import com.utility.billing.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepo;
    @Mock private BillRepository billRepo;
    @Mock private AccountRepository accountRepo;

    private PaymentService paymentService;

    @BeforeEach
    void setup() {
        paymentService = new PaymentService(paymentRepo, billRepo, accountRepo);
    }

    @Test
    void processPayment_allocatesToOldestBillFirst() {
        Account account = new Account("ACC-100", "Test User",
                Account.ServiceClass.RESIDENTIAL, 1);
        account.setBalance(new BigDecimal("200.00"));

        Bill oldBill = new Bill("BILL-OLD", account, LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));
        oldBill.setUsageCharge(new BigDecimal("80.00"));
        oldBill.setBaseCharge(new BigDecimal("12.50"));
        oldBill.setTaxAmount(new BigDecimal("8.00"));
        oldBill.setTotalAmount(new BigDecimal("100.50"));
        oldBill.setBalanceDue(new BigDecimal("100.50"));
        oldBill.setStatus(Bill.BillStatus.SENT);

        Bill newBill = new Bill("BILL-NEW", account, LocalDate.now().minusDays(30), LocalDate.now());
        newBill.setTotalAmount(new BigDecimal("99.50"));
        newBill.setBalanceDue(new BigDecimal("99.50"));
        newBill.setStatus(Bill.BillStatus.SENT);

        when(accountRepo.findByAccountNumber("ACC-100")).thenReturn(Optional.of(account));
        when(billRepo.findUnpaidBills(any())).thenReturn(List.of(oldBill, newBill));
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(billRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = paymentService.processPayment("ACC-100",
                new BigDecimal("150.00"), Payment.PaymentMethod.ACH);

        assertNotNull(payment);
        assertEquals(Payment.PaymentStatus.POSTED, payment.getStatus());
        // Old bill should be fully paid, new bill partially
        assertEquals(Bill.BillStatus.PAID, oldBill.getStatus());
        assertEquals(new BigDecimal("50.00"), newBill.getBalanceDue().subtract(new BigDecimal("0.00")));
    }

    @Test
    void processPayment_throwsForInvalidAccount() {
        when(accountRepo.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.processPayment("INVALID", BigDecimal.TEN, Payment.PaymentMethod.CHECK));
    }

    @Test
    void reversePayment_updatesStatusAndBalance() {
        Account account = new Account("ACC-200", "Reverse Test",
                Account.ServiceClass.COMMERCIAL, 2);
        account.setBalance(new BigDecimal("50.00"));

        Payment payment = new Payment("PAY-REV", account, new BigDecimal("75.00"), Payment.PaymentMethod.CHECK);
        payment.markPosted();

        when(paymentRepo.findByPaymentRef("PAY-REV")).thenReturn(Optional.of(payment));
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment reversed = paymentService.reversePayment("PAY-REV");

        assertEquals(Payment.PaymentStatus.REVERSED, reversed.getStatus());
        assertEquals(new BigDecimal("125.00"), account.getBalance());
    }
}
