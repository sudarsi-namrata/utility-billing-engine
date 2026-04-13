package com.utility.billing.batch;

import com.utility.billing.model.Bill;
import com.utility.billing.model.Account;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Nightly aging job: marks overdue bills and flags accounts for disconnect.
 * Runs as a single tasklet since it's a bulk update, not chunk processing.
 */
@Configuration
public class AgingJobConfig {

    private static final Logger log = LoggerFactory.getLogger(AgingJobConfig.class);

    private final BillRepository billRepo;
    private final AccountRepository accountRepo;

    public AgingJobConfig(BillRepository billRepo, AccountRepository accountRepo) {
        this.billRepo = billRepo;
        this.accountRepo = accountRepo;
    }

    @Bean
    public Job agingJob(JobRepository jobRepository, Step agingStep) {
        return new JobBuilder("agingJob", jobRepository)
                .start(agingStep)
                .build();
    }

    @Bean
    public Step agingStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("agingStep", jobRepository)
                .tasklet(agingTasklet(), txManager)
                .build();
    }

    private Tasklet agingTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate today = LocalDate.now();

            // Mark sent bills as overdue if past due date
            List<Bill> overdueBills = billRepo.findOverdueBills(today);
            int overdueCount = 0;
            for (Bill bill : overdueBills) {
                bill.setStatus(Bill.BillStatus.OVERDUE);
                billRepo.save(bill);
                overdueCount++;

                // If bill is 90+ days overdue, flag account for disconnect review
                if (bill.getDueDate().plusDays(90).isBefore(today)) {
                    Account account = bill.getAccount();
                    if (account.getStatus() == Account.AccountStatus.ACTIVE) {
                        account.setStatus(Account.AccountStatus.PENDING_DISCONNECT);
                        accountRepo.save(account);
                        log.warn("Account {} flagged for disconnect - bill {} is 90+ days overdue",
                                account.getAccountNumber(), bill.getBillNumber());
                    }
                }
            }

            log.info("Aging job complete: {} bills marked overdue", overdueCount);
            return RepeatStatus.FINISHED;
        };
    }
}
