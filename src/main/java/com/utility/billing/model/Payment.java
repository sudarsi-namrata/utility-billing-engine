package com.utility.billing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENTS", indexes = {
    @Index(name = "idx_payments_account", columnList = "ACCOUNT_ID"),
    @Index(name = "idx_payments_bill", columnList = "BILL_ID")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq")
    @SequenceGenerator(name = "payment_seq", sequenceName = "PAYMENT_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "PAYMENT_REF", unique = true, nullable = false)
    private String paymentRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BILL_ID")
    private Bill bill;

    @Column(name = "AMOUNT", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_METHOD")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "TRANSACTION_DATE")
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Column(name = "POSTED_DATE")
    private LocalDateTime postedDate;

    @Column(name = "EXTERNAL_REF")
    private String externalRef; // check number, transaction ID, etc.

    public Payment() {}

    public Payment(String paymentRef, Account account, BigDecimal amount, PaymentMethod method) {
        this.paymentRef = paymentRef;
        this.account = account;
        this.amount = amount;
        this.paymentMethod = method;
    }

    public void markPosted() {
        this.status = PaymentStatus.POSTED;
        this.postedDate = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getPaymentRef() { return paymentRef; }
    public Account getAccount() { return account; }
    public Bill getBill() { return bill; }
    public void setBill(Bill bill) { this.bill = bill; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public LocalDateTime getPostedDate() { return postedDate; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }

    public enum PaymentMethod {
        CHECK, ACH, CREDIT_CARD, CASH, BANK_DRAFT, ONLINE
    }

    public enum PaymentStatus {
        PENDING, POSTED, REVERSED, RETURNED, CANCELLED
    }
}
