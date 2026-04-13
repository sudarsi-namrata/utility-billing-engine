package com.utility.billing.controller;

import com.utility.billing.model.Bill;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.service.BillingEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingEngine billingEngine;
    private final BillRepository billRepo;

    public BillingController(BillingEngine billingEngine, BillRepository billRepo) {
        this.billingEngine = billingEngine;
        this.billRepo = billRepo;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runBilling(@RequestParam(defaultValue = "1") int cycle) {
        List<Bill> bills = billingEngine.runBillingCycle(cycle);
        return ResponseEntity.ok(Map.of(
                "cycle", cycle,
                "billsGenerated", bills.size(),
                "totalAmount", bills.stream()
                        .map(Bill::getTotalAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
        ));
    }

    @GetMapping("/bills/{accountId}")
    public ResponseEntity<List<Bill>> getBills(@PathVariable Long accountId) {
        return ResponseEntity.ok(billRepo.findByAccountIdOrderByPeriodEndDesc(accountId));
    }

    @GetMapping("/bills/stats")
    public ResponseEntity<Map<String, Long>> billStats() {
        return ResponseEntity.ok(Map.of(
                "pending", billRepo.countByStatus(Bill.BillStatus.PENDING),
                "sent", billRepo.countByStatus(Bill.BillStatus.SENT),
                "paid", billRepo.countByStatus(Bill.BillStatus.PAID),
                "overdue", billRepo.countByStatus(Bill.BillStatus.OVERDUE)
        ));
    }
}
