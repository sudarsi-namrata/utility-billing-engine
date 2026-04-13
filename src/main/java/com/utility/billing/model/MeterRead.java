package com.utility.billing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "METER_READS", indexes = {
    @Index(name = "idx_meter_reads_account", columnList = "ACCOUNT_ID"),
    @Index(name = "idx_meter_reads_date", columnList = "READ_DATE")
})
public class MeterRead {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meter_read_seq")
    @SequenceGenerator(name = "meter_read_seq", sequenceName = "METER_READ_SEQ", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID", nullable = false)
    private Account account;

    @Column(name = "METER_NUMBER", nullable = false)
    private String meterNumber;

    @Column(name = "READING_VALUE", precision = 14, scale = 3, nullable = false)
    private BigDecimal readingValue;

    @Column(name = "PREVIOUS_READING", precision = 14, scale = 3)
    private BigDecimal previousReading;

    @Column(name = "USAGE_KWH", precision = 14, scale = 3)
    private BigDecimal usageKwh;

    @Enumerated(EnumType.STRING)
    @Column(name = "READ_TYPE", nullable = false)
    private ReadType readType;

    @Column(name = "READ_DATE", nullable = false)
    private LocalDateTime readDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "VALIDATION_STATUS")
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @Column(name = "VALIDATION_MESSAGE")
    private String validationMessage;

    @Column(name = "BILLED")
    private boolean billed = false;

    public MeterRead() {}

    public MeterRead(Account account, String meterNumber, BigDecimal readingValue,
                     ReadType readType, LocalDateTime readDate) {
        this.account = account;
        this.meterNumber = meterNumber;
        this.readingValue = readingValue;
        this.readType = readType;
        this.readDate = readDate;
    }

    public void calculateUsage() {
        if (previousReading != null && readingValue != null) {
            this.usageKwh = readingValue.subtract(previousReading);
            // Handle meter rollover (e.g., 99999 -> 00123)
            if (this.usageKwh.compareTo(BigDecimal.ZERO) < 0) {
                this.usageKwh = new BigDecimal("100000").add(this.usageKwh);
            }
        }
    }

    // Getters and setters
    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public String getMeterNumber() { return meterNumber; }
    public BigDecimal getReadingValue() { return readingValue; }
    public void setReadingValue(BigDecimal readingValue) { this.readingValue = readingValue; }
    public BigDecimal getPreviousReading() { return previousReading; }
    public void setPreviousReading(BigDecimal previousReading) { this.previousReading = previousReading; }
    public BigDecimal getUsageKwh() { return usageKwh; }
    public void setUsageKwh(BigDecimal usageKwh) { this.usageKwh = usageKwh; }
    public ReadType getReadType() { return readType; }
    public LocalDateTime getReadDate() { return readDate; }
    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus s) { this.validationStatus = s; }
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String msg) { this.validationMessage = msg; }
    public boolean isBilled() { return billed; }
    public void setBilled(boolean billed) { this.billed = billed; }

    public enum ReadType {
        ACTUAL, ESTIMATED, CUSTOMER_READ
    }

    public enum ValidationStatus {
        PENDING, VALID, HIGH_READ, LOW_READ, ZERO_USAGE, REJECTED
    }
}
