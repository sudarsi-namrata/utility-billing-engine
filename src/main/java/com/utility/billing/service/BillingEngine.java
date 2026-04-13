package com.utility.billing.service;

import com.utility.billing.model.*;
import com.utility.billing.repository.*;
import com.utility.billing.scripting.GroovyScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BillingEngine {

    private static final Logger log = LoggerFactory.getLogger(BillingEngine.class);
    private static final AtomicLong billCounter = new AtomicLong(System.currentTimeMillis());

    private final AccountRepository accountRepo;
    private final MeterReadRepository meterReadRepo;
    private final BillRepository billRepo;
    private final TariffRepository tariffRepo;
    private final GroovyScriptEngine scriptEngine;

    public BillingEngine(AccountRepository accountRepo, MeterReadRepository meterReadRepo,
                         BillRepository billRepo, TariffRepository tariffRepo,
                         GroovyScriptEngine scriptEngine) {
        this.accountRepo = accountRepo;
        this.meterReadRepo = meterReadRepo;
        this.billRepo = billRepo;
        this.tariffRepo = tariffRepo;
        this.scriptEngine = scriptEngine;
    }

    /**
     * Run billing for all accounts in a given cycle.
     * This is the core meter-to-cash flow: reads -> calculation -> bill.
     */
    @Transactional
    public List<Bill> runBillingCycle(int billingCycle) {
        List<Account> accounts = accountRepo.findBillableAccounts(billingCycle, Account.AccountStatus.ACTIVE);
        log.info("Starting billing run for cycle {} - {} accounts", billingCycle, accounts.size());

        List<Bill> generatedBills = new ArrayList<>();

        for (Account account : accounts) {
            try {
                Bill bill = generateBill(account);
                if (bill != null) {
                    generatedBills.add(bill);
                }
            } catch (Exception e) {
                log.error("Billing failed for account {}: {}", account.getAccountNumber(), e.getMessage());
                // Continue with other accounts - don't fail the whole run
            }
        }

        log.info("Billing run complete: {} bills generated out of {} accounts", generatedBills.size(), accounts.size());
        return generatedBills;
    }

    @Transactional
    public Bill generateBill(Account account) {
        List<MeterRead> unbilledReads = meterReadRepo.findUnbilledValidReads(account.getId());
        if (unbilledReads.isEmpty()) {
            log.debug("No unbilled reads for account {}", account.getAccountNumber());
            return null;
        }

        // Sum usage across all unbilled reads
        BigDecimal totalUsage = unbilledReads.stream()
                .map(MeterRead::getUsageKwh)
                .filter(u -> u != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Determine billing period from read dates
        LocalDate periodStart = unbilledReads.get(0).getReadDate().toLocalDate();
        LocalDate periodEnd = unbilledReads.get(unbilledReads.size() - 1).getReadDate().toLocalDate();

        // Look up active tariff
        Tariff tariff = tariffRepo.findActiveTariff(account.getServiceClass(), LocalDate.now())
                .orElseThrow(() -> new IllegalStateException(
                        "No active tariff for service class: " + account.getServiceClass()));

        // Create the bill
        String billNumber = "BILL-" + DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now())
                + "-" + billCounter.incrementAndGet();
        Bill bill = new Bill(billNumber, account, periodStart, periodEnd);
        bill.setUsageKwh(totalUsage);
        bill.setTariffCode(tariff.getTariffCode());

        // Calculate charges
        BigDecimal usageCharge;
        if (tariff.getGroovyScriptName() != null && scriptEngine.hasScript(tariff.getGroovyScriptName())) {
            // Use Groovy script for custom rate calculation
            MeterRead latestRead = unbilledReads.get(unbilledReads.size() - 1);
            usageCharge = scriptEngine.executeRateScript(tariff.getGroovyScriptName(), latestRead, account, tariff);
        } else {
            // Default tiered calculation
            usageCharge = calculateTieredCharge(totalUsage, tariff);
        }

        bill.setBaseCharge(tariff.getBaseCharge());
        bill.setUsageCharge(usageCharge);

        BigDecimal subtotal = tariff.getBaseCharge().add(usageCharge);
        BigDecimal tax = subtotal.multiply(tariff.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
        bill.setTaxAmount(tax);
        bill.calculateTotal();

        billRepo.save(bill);

        // Mark reads as billed
        unbilledReads.forEach(read -> {
            read.setBilled(true);
            meterReadRepo.save(read);
        });

        // Update account
        account.setBalance(account.getBalance().add(bill.getTotalAmount()));
        account.setLastBillDate(LocalDate.now());
        accountRepo.save(account);

        log.info("Generated bill {} for account {} - {} kWh, ${}", billNumber,
                account.getAccountNumber(), totalUsage, bill.getTotalAmount());
        return bill;
    }

    private BigDecimal calculateTieredCharge(BigDecimal totalUsage, Tariff tariff) {
        BigDecimal charge = BigDecimal.ZERO;
        BigDecimal remaining = totalUsage;

        // Tier 1
        if (tariff.getTier1Limit() != null && tariff.getTier1Rate() != null) {
            BigDecimal tier1Usage = remaining.min(BigDecimal.valueOf(tariff.getTier1Limit()));
            charge = charge.add(tier1Usage.multiply(tariff.getTier1Rate()));
            remaining = remaining.subtract(tier1Usage).max(BigDecimal.ZERO);
        }

        // Tier 2
        if (tariff.getTier2Limit() != null && tariff.getTier2Rate() != null && remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tier2Limit = BigDecimal.valueOf(tariff.getTier2Limit() - tariff.getTier1Limit());
            BigDecimal tier2Usage = remaining.min(tier2Limit);
            charge = charge.add(tier2Usage.multiply(tariff.getTier2Rate()));
            remaining = remaining.subtract(tier2Usage).max(BigDecimal.ZERO);
        }

        // Tier 3 (everything above)
        if (tariff.getTier3Rate() != null && remaining.compareTo(BigDecimal.ZERO) > 0) {
            charge = charge.add(remaining.multiply(tariff.getTier3Rate()));
        }

        return charge.setScale(2, RoundingMode.HALF_UP);
    }
}
