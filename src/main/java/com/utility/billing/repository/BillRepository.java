package com.utility.billing.repository;

import com.utility.billing.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByAccountIdOrderByPeriodEndDesc(Long accountId);

    @Query("SELECT b FROM Bill b WHERE b.account.id = :accountId AND b.status NOT IN ('PAID', 'CANCELLED') " +
           "ORDER BY b.periodEnd ASC")
    List<Bill> findUnpaidBills(@Param("accountId") Long accountId);

    List<Bill> findByStatusAndDueDateBefore(Bill.BillStatus status, LocalDate date);

    @Query("SELECT b FROM Bill b WHERE b.status = 'SENT' AND b.dueDate < :date AND b.balanceDue > 0")
    List<Bill> findOverdueBills(@Param("date") LocalDate date);

    long countByStatus(Bill.BillStatus status);
}
