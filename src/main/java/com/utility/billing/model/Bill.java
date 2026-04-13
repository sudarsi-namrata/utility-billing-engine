package com.utility.billing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "BILLS", indexes = {
    @Index(name = "idx_bills_account", columnList = "ACCOUNT_ID"),
    @Index(name = "idx_bills_status", columnList = "STATUS")
})
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bill_seq")
    @SequenceGenerator(name = "bill_seq", sequenceName = "BILL_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "BILL_NUMBER", unique = true, nullable = false)
    private String billNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID", nullable = false)
    private Account account;

    @Column(name = "PERIOD_START", nullable = false)
    private LocalDate periodStart;

    @Column(name = "PERIOD_END", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "USAGE_KWH", precision = 14, scale = 3)
    private BigDecimal usageKwh;

    @Column(name = "BASE_CHARGE", precision = 12, scale = 2)
    private BigDecimal baseCharge = BigDecimal.ZERO;

    @Column(name = "USAGE_CHARGE", precision = 12, scale = 2)
    private BigDecimal usageCharge = BigDecimal.ZERO;

    @Column(name = "TAX_AMOUNT", precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "TOTAL_AMOUNT", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "AMOUNT_PAID", precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "BALANCE_DUE", precision = 12, scale = 2)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private BillStatus status = BillStatus.PENDING;

    @Column(name = "TARIFF_CODE")
    private String tariffCode;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Bill() {}

    public Bill(String billNumber, Account account, LocalDate periodStart, LocalDate periodEnd) {
        this.billNumber = billNumber;
        this.account = account;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dueDate = periodEnd.plusDays(21); // 21-day payment window
    }

    public void calculateTotal() {
        this.totalAmount = baseCharge.add(usageCharge).add(taxAmount);
        this.balanceDue = totalAmount.subtract(amountPaid);
    }

    public void applyPayment(BigDecimal amount) {
        this.amountPaid = this.amountPaid.add(amount);
        this.balanceDue = this.totalAmount.subtract(this.amountPaid);
        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = BillStatus.PAID;
            this.balanceDue = BigDecimal.ZERO;
        }
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getBillNumber() { return billNumber; }
    public Account getAccount() { return account; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getUsageKwh() { return usageKwh; }
    public void setUsageKwh(BigDecimal usageKwh) { this.usageKwh = usageKwh; }
    public BigDecimal getBaseCharge() { return baseCharge; }
    public void setBaseCharge(BigDecimal baseCharge) { this.baseCharge = baseCharge; }
    public BigDecimal getUsageCharge() { return usageCharge; }
    public void setUsageCharge(BigDecimal usageCharge) { this.usageCharge = usageCharge; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getBalanceDue() { return balanceDue; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }
    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public enum BillStatus {
        PENDING, APPROVED, SENT, PAID, PARTIAL_PAID, OVERDUE, CANCELLED, WRITE_OFF
    }
}
