package com.utility.billing.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "ACCOUNTS")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq")
    @SequenceGenerator(name = "account_seq", sequenceName = "ACCOUNT_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "ACCOUNT_NUMBER", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "CUSTOMER_NAME", nullable = false)
    private String customerName;

    @Column(name = "SERVICE_ADDRESS")
    private String serviceAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "SERVICE_CLASS", nullable = false)
    private ServiceClass serviceClass;

    @Column(name = "BILLING_CYCLE", nullable = false)
    private Integer billingCycle; // 1-20, determines when the account gets billed

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "METER_NUMBER")
    private String meterNumber;

    @Column(name = "BALANCE", precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "LAST_BILL_DATE")
    private LocalDate lastBillDate;

    @Column(name = "CREATED_AT")
    private LocalDate createdAt = LocalDate.now();

    public Account() {}

    public Account(String accountNumber, String customerName, ServiceClass serviceClass, int billingCycle) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.serviceClass = serviceClass;
        this.billingCycle = billingCycle;
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getServiceAddress() { return serviceAddress; }
    public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }
    public ServiceClass getServiceClass() { return serviceClass; }
    public void setServiceClass(ServiceClass serviceClass) { this.serviceClass = serviceClass; }
    public Integer getBillingCycle() { return billingCycle; }
    public void setBillingCycle(Integer billingCycle) { this.billingCycle = billingCycle; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public String getMeterNumber() { return meterNumber; }
    public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public LocalDate getLastBillDate() { return lastBillDate; }
    public void setLastBillDate(LocalDate lastBillDate) { this.lastBillDate = lastBillDate; }
    public LocalDate getCreatedAt() { return createdAt; }

    public enum ServiceClass {
        RESIDENTIAL, COMMERCIAL, INDUSTRIAL, AGRICULTURAL
    }

    public enum AccountStatus {
        ACTIVE, SUSPENDED, CLOSED, PENDING_DISCONNECT
    }
}
