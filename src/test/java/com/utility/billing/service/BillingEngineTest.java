package com.utility.billing.service;

import com.utility.billing.model.*;
import com.utility.billing.repository.*;
import com.utility.billing.scripting.GroovyScriptEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingEngineTest {

    @Mock private AccountRepository accountRepo;
    @Mock private MeterReadRepository meterReadRepo;
    @Mock private BillRepository billRepo;
    @Mock private TariffRepository tariffRepo;
    @Mock private GroovyScriptEngine scriptEngine;

    private BillingEngine billingEngine;

    @BeforeEach
    void setup() {
        billingEngine = new BillingEngine(accountRepo, meterReadRepo, billRepo, tariffRepo, scriptEngine);
    }

    @Test
    void generateBill_calculatesThreeTierCharges() {
        Account account = new Account("ACC-001", "John Doe",
                Account.ServiceClass.RESIDENTIAL, 1);

        // 750 kWh usage - spans tier 1 (500) and tier 2 (250)
        MeterRead read = new MeterRead(account, "MTR-001", new BigDecimal("10750"),
                MeterRead.ReadType.ACTUAL, LocalDateTime.now());
        read.setPreviousReading(new BigDecimal("10000"));
        read.setUsageKwh(new BigDecimal("750"));
        read.setValidationStatus(MeterRead.ValidationStatus.VALID);

        Tariff tariff = new Tariff();
        tariff.setTariffCode("RES-STANDARD");
        tariff.setServiceClass(Account.ServiceClass.RESIDENTIAL);
        tariff.setBaseCharge(new BigDecimal("12.50"));
        tariff.setTier1Limit(500);
        tariff.setTier1Rate(new BigDecimal("0.08"));
        tariff.setTier2Limit(1000);
        tariff.setTier2Rate(new BigDecimal("0.12"));
        tariff.setTier3Rate(new BigDecimal("0.18"));
        tariff.setTaxRate(new BigDecimal("0.087"));

        when(meterReadRepo.findUnbilledValidReads(any())).thenReturn(List.of(read));
        when(tariffRepo.findActiveTariff(any(), any())).thenReturn(Optional.of(tariff));
        when(billRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meterReadRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Bill bill = billingEngine.generateBill(account);

        assertNotNull(bill);
        assertEquals(new BigDecimal("750.000"), bill.getUsageKwh());
        // Tier 1: 500 * 0.08 = 40.00, Tier 2: 250 * 0.12 = 30.00 => 70.00
        assertEquals(new BigDecimal("70.00"), bill.getUsageCharge());
        assertEquals(new BigDecimal("12.50"), bill.getBaseCharge());
        // Tax: (12.50 + 70.00) * 0.087 = 7.18
        assertTrue(bill.getTaxAmount().compareTo(new BigDecimal("7.17")) >= 0);
        assertTrue(bill.getTotalAmount().compareTo(new BigDecimal("89")) > 0);

        verify(meterReadRepo).save(argThat(r -> r.isBilled()));
    }

    @Test
    void generateBill_returnsNull_whenNoUnbilledReads() {
        Account account = new Account("ACC-002", "Jane Smith",
                Account.ServiceClass.COMMERCIAL, 5);
        when(meterReadRepo.findUnbilledValidReads(any())).thenReturn(List.of());

        Bill bill = billingEngine.generateBill(account);

        assertNull(bill);
        verify(billRepo, never()).save(any());
    }

    @Test
    void generateBill_usesGroovyScript_whenConfigured() {
        Account account = new Account("ACC-003", "Bob", Account.ServiceClass.RESIDENTIAL, 1);

        MeterRead read = new MeterRead(account, "MTR-003", new BigDecimal("11000"),
                MeterRead.ReadType.ACTUAL, LocalDateTime.now());
        read.setUsageKwh(new BigDecimal("600"));
        read.setValidationStatus(MeterRead.ValidationStatus.VALID);

        Tariff tariff = new Tariff();
        tariff.setTariffCode("RES-CUSTOM");
        tariff.setServiceClass(Account.ServiceClass.RESIDENTIAL);
        tariff.setBaseCharge(new BigDecimal("15.00"));
        tariff.setTaxRate(new BigDecimal("0.05"));
        tariff.setGroovyScriptName("custom-rate");

        when(meterReadRepo.findUnbilledValidReads(any())).thenReturn(List.of(read));
        when(tariffRepo.findActiveTariff(any(), any())).thenReturn(Optional.of(tariff));
        when(scriptEngine.hasScript("custom-rate")).thenReturn(true);
        when(scriptEngine.executeRateScript(eq("custom-rate"), any(), any(), any()))
                .thenReturn(new BigDecimal("55.00"));
        when(billRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meterReadRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Bill bill = billingEngine.generateBill(account);

        assertNotNull(bill);
        assertEquals(new BigDecimal("55.00"), bill.getUsageCharge());
        verify(scriptEngine).executeRateScript(eq("custom-rate"), any(), any(), any());
    }

    @Test
    void runBillingCycle_processesMultipleAccounts() {
        Account acc1 = new Account("ACC-010", "Alice", Account.ServiceClass.RESIDENTIAL, 3);
        Account acc2 = new Account("ACC-011", "Bob", Account.ServiceClass.COMMERCIAL, 3);

        when(accountRepo.findBillableAccounts(3, Account.AccountStatus.ACTIVE))
                .thenReturn(List.of(acc1, acc2));
        when(meterReadRepo.findUnbilledValidReads(any())).thenReturn(List.of());

        List<Bill> bills = billingEngine.runBillingCycle(3);

        // Both return null (no unbilled reads), so 0 bills
        assertEquals(0, bills.size());
    }
}
