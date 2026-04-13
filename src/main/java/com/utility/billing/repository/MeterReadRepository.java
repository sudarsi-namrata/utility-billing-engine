package com.utility.billing.repository;

import com.utility.billing.model.MeterRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeterReadRepository extends JpaRepository<MeterRead, Long> {

    List<MeterRead> findByAccountIdOrderByReadDateDesc(Long accountId);

    @Query("SELECT mr FROM MeterRead mr WHERE mr.account.id = :accountId AND mr.billed = false " +
           "AND mr.validationStatus = 'VALID' ORDER BY mr.readDate ASC")
    List<MeterRead> findUnbilledValidReads(@Param("accountId") Long accountId);

    @Query("SELECT mr FROM MeterRead mr WHERE mr.account.id = :accountId " +
           "ORDER BY mr.readDate DESC LIMIT 1")
    Optional<MeterRead> findLatestByAccountId(@Param("accountId") Long accountId);

    List<MeterRead> findByValidationStatusAndReadDateBetween(
            MeterRead.ValidationStatus status, LocalDateTime start, LocalDateTime end);
}
