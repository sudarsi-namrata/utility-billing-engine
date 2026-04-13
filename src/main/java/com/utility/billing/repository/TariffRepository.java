package com.utility.billing.repository;

import com.utility.billing.model.Account;
import com.utility.billing.model.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    @Query("SELECT t FROM Tariff t WHERE t.serviceClass = :sc " +
           "AND (t.effectiveDate IS NULL OR t.effectiveDate <= :date) " +
           "AND (t.expiryDate IS NULL OR t.expiryDate >= :date) " +
           "ORDER BY t.effectiveDate DESC LIMIT 1")
    Optional<Tariff> findActiveTariff(@Param("sc") Account.ServiceClass sc, @Param("date") LocalDate date);

    Optional<Tariff> findByTariffCode(String tariffCode);
}
