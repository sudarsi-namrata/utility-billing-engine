package com.utility.billing.service;

import com.utility.billing.model.Account;
import com.utility.billing.model.MeterRead;
import com.utility.billing.repository.AccountRepository;
import com.utility.billing.repository.MeterReadRepository;
import com.utility.billing.scripting.GroovyScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MeterIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MeterIngestionService.class);

    // High/low thresholds — reads outside 3x average are flagged
    private static final BigDecimal HIGH_MULTIPLIER = new BigDecimal("3.0");
    private static final BigDecimal LOW_MULTIPLIER = new BigDecimal("0.1");

    private final MeterReadRepository meterReadRepo;
    private final AccountRepository accountRepo;
    private final GroovyScriptEngine scriptEngine;

    public MeterIngestionService(MeterReadRepository meterReadRepo, AccountRepository accountRepo,
                                  GroovyScriptEngine scriptEngine) {
        this.meterReadRepo = meterReadRepo;
        this.accountRepo = accountRepo;
        this.scriptEngine = scriptEngine;
    }

    @Transactional
    public MeterRead ingestRead(String accountNumber, BigDecimal readingValue,
                                 MeterRead.ReadType readType, LocalDateTime readDate) {
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        var read = new MeterRead(account, account.getMeterNumber(), readingValue, readType, readDate);

        // Attach previous reading for usage calculation
        meterReadRepo.findLatestByAccountId(account.getId()).ifPresent(prev -> {
            read.setPreviousReading(prev.getReadingValue());
            read.calculateUsage();
        });

        validateRead(read);

        meterReadRepo.save(read);
        log.info("Ingested meter read for account {} - {} kWh, status={}",
                accountNumber, read.getUsageKwh(), read.getValidationStatus());
        return read;
    }

    @Transactional
    public List<MeterRead> ingestBatch(List<MeterReadRequest> requests) {
        return requests.stream()
                .map(req -> {
                    try {
                        return ingestRead(req.accountNumber(), req.readingValue(),
                                req.readType(), req.readDate());
                    } catch (Exception e) {
                        log.error("Failed to ingest read for account {}: {}", req.accountNumber(), e.getMessage());
                        return null;
                    }
                })
                .filter(r -> r != null)
                .toList();
    }

    private void validateRead(MeterRead read) {
        // Zero usage check
        if (read.getUsageKwh() != null && read.getUsageKwh().compareTo(BigDecimal.ZERO) == 0
                && read.getReadType() == MeterRead.ReadType.ACTUAL) {
            read.setValidationStatus(MeterRead.ValidationStatus.ZERO_USAGE);
            read.setValidationMessage("Zero usage on actual read - verify meter");
            return;
        }

        // High/low read check against previous
        if (read.getUsageKwh() != null && read.getPreviousReading() != null) {
            BigDecimal avgUsage = read.getPreviousReading().multiply(new BigDecimal("0.033")); // rough monthly ~3.3% of reading
            if (avgUsage.compareTo(BigDecimal.ZERO) > 0) {
                if (read.getUsageKwh().compareTo(avgUsage.multiply(HIGH_MULTIPLIER)) > 0) {
                    read.setValidationStatus(MeterRead.ValidationStatus.HIGH_READ);
                    read.setValidationMessage("Usage " + read.getUsageKwh() + " exceeds 3x threshold");
                    return;
                }
                if (read.getUsageKwh().compareTo(avgUsage.multiply(LOW_MULTIPLIER)) < 0) {
                    read.setValidationStatus(MeterRead.ValidationStatus.LOW_READ);
                    read.setValidationMessage("Usage " + read.getUsageKwh() + " below 10% threshold");
                    return;
                }
            }
        }

        // Custom Groovy validation script if configured
        if (scriptEngine.hasScript("meter-validation")) {
            MeterRead prev = read.getPreviousReading() != null ? read : null;
            String error = scriptEngine.executeValidationScript("meter-validation", read, prev);
            if (error != null) {
                read.setValidationStatus(MeterRead.ValidationStatus.REJECTED);
                read.setValidationMessage(error);
                return;
            }
        }

        read.setValidationStatus(MeterRead.ValidationStatus.VALID);
    }

    public record MeterReadRequest(
            String accountNumber,
            BigDecimal readingValue,
            MeterRead.ReadType readType,
            LocalDateTime readDate
    ) {}
}
