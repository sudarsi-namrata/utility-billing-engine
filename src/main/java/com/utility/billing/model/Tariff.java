package com.utility.billing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TARIFFS")
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tariff_seq")
    @SequenceGenerator(name = "tariff_seq", sequenceName = "TARIFF_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "TARIFF_CODE", nullable = false)
    private String tariffCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "SERVICE_CLASS", nullable = false)
    private Account.ServiceClass serviceClass;

    @Column(name = "BASE_CHARGE", precision = 10, scale = 4)
    private BigDecimal baseCharge; // monthly fixed charge

    @Column(name = "TIER1_LIMIT")
    private Integer tier1Limit; // kWh threshold for tier 1

    @Column(name = "TIER1_RATE", precision = 10, scale = 6)
    private BigDecimal tier1Rate; // $/kWh for tier 1

    @Column(name = "TIER2_LIMIT")
    private Integer tier2Limit;

    @Column(name = "TIER2_RATE", precision = 10, scale = 6)
    private BigDecimal tier2Rate;

    @Column(name = "TIER3_RATE", precision = 10, scale = 6)
    private BigDecimal tier3Rate; // everything above tier 2

    @Column(name = "TAX_RATE", precision = 8, scale = 6)
    private BigDecimal taxRate;

    @Column(name = "SEASON")
    private String season; // SUMMER, WINTER, or null for year-round

    @Column(name = "EFFECTIVE_DATE")
    private LocalDate effectiveDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDate expiryDate;

    @Column(name = "GROOVY_SCRIPT_NAME")
    private String groovyScriptName; // optional override script

    public Tariff() {}

    public boolean isActive() {
        LocalDate now = LocalDate.now();
        return (effectiveDate == null || !now.isBefore(effectiveDate))
            && (expiryDate == null || !now.isAfter(expiryDate));
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }
    public Account.ServiceClass getServiceClass() { return serviceClass; }
    public void setServiceClass(Account.ServiceClass sc) { this.serviceClass = sc; }
    public BigDecimal getBaseCharge() { return baseCharge; }
    public void setBaseCharge(BigDecimal baseCharge) { this.baseCharge = baseCharge; }
    public Integer getTier1Limit() { return tier1Limit; }
    public void setTier1Limit(Integer tier1Limit) { this.tier1Limit = tier1Limit; }
    public BigDecimal getTier1Rate() { return tier1Rate; }
    public void setTier1Rate(BigDecimal tier1Rate) { this.tier1Rate = tier1Rate; }
    public Integer getTier2Limit() { return tier2Limit; }
    public void setTier2Limit(Integer tier2Limit) { this.tier2Limit = tier2Limit; }
    public BigDecimal getTier2Rate() { return tier2Rate; }
    public void setTier2Rate(BigDecimal tier2Rate) { this.tier2Rate = tier2Rate; }
    public BigDecimal getTier3Rate() { return tier3Rate; }
    public void setTier3Rate(BigDecimal tier3Rate) { this.tier3Rate = tier3Rate; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getGroovyScriptName() { return groovyScriptName; }
    public void setGroovyScriptName(String groovyScriptName) { this.groovyScriptName = groovyScriptName; }
}
