package com.utility.billing.repository;

import com.utility.billing.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByBillingCycleAndStatus(Integer billingCycle, Account.AccountStatus status);

    @Query("SELECT a FROM Account a WHERE a.status = :status AND a.billingCycle = :cycle ORDER BY a.accountNumber")
    List<Account> findBillableAccounts(@Param("cycle") int cycle, @Param("status") Account.AccountStatus status);

    List<Account> findByServiceClass(Account.ServiceClass serviceClass);
}
