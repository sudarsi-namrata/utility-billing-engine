package com.utility.billing.batch;

import com.utility.billing.model.Account;
import com.utility.billing.model.Bill;
import com.utility.billing.service.BillingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.utility.billing.repository.AccountRepository;

import java.util.List;

@Configuration
public class BillingRunJobConfig {

    private static final Logger log = LoggerFactory.getLogger(BillingRunJobConfig.class);

    private final AccountRepository accountRepo;
    private final BillingEngine billingEngine;

    public BillingRunJobConfig(AccountRepository accountRepo, BillingEngine billingEngine) {
        this.accountRepo = accountRepo;
        this.billingEngine = billingEngine;
    }

    @Bean
    public Job billingRunJob(JobRepository jobRepository, Step billingStep) {
        return new JobBuilder("billingRunJob", jobRepository)
                .start(billingStep)
                .build();
    }

    @Bean
    public Step billingStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("billingStep", jobRepository)
                .<Account, Bill>chunk(50, txManager)
                .reader(accountReader())
                .processor(billingProcessor())
                .writer(billingWriter())
                .faultTolerant()
                .skipLimit(100) // allow up to 100 failed accounts per run
                .skip(Exception.class)
                .retryLimit(2)
                .retry(Exception.class)
                .build();
    }

    private ItemReader<Account> accountReader() {
        // In production, this would be a JpaPagingItemReader for cursor-based reading.
        // Simplified here with ListItemReader for clarity.
        List<Account> accounts = accountRepo.findByBillingCycleAndStatus(
                getCurrentBillingCycle(), Account.AccountStatus.ACTIVE);
        return new ListItemReader<>(accounts);
    }

    private ItemProcessor<Account, Bill> billingProcessor() {
        return account -> {
            try {
                return billingEngine.generateBill(account);
            } catch (Exception e) {
                log.error("Failed to generate bill for {}: {}", account.getAccountNumber(), e.getMessage());
                throw e; // let skip/retry handle it
            }
        };
    }

    private ItemWriter<Bill> billingWriter() {
        return bills -> {
            long count = bills.getItems().stream().filter(b -> b != null).count();
            log.info("Billing step wrote {} bills", count);
        };
    }

    private int getCurrentBillingCycle() {
        // Billing cycle 1-20, determined by day of month
        return (java.time.LocalDate.now().getDayOfMonth() % 20) + 1;
    }
}
