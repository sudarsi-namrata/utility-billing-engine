package com.utility.billing.controller;

import com.utility.billing.model.MeterRead;
import com.utility.billing.repository.MeterReadRepository;
import com.utility.billing.service.MeterIngestionService;
import com.utility.billing.service.MeterIngestionService.MeterReadRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/meters")
public class MeterController {

    private final MeterIngestionService ingestionService;
    private final MeterReadRepository meterReadRepo;

    public MeterController(MeterIngestionService ingestionService, MeterReadRepository meterReadRepo) {
        this.ingestionService = ingestionService;
        this.meterReadRepo = meterReadRepo;
    }

    @PostMapping("/reads")
    public ResponseEntity<MeterRead> submitRead(@RequestBody MeterReadDto dto) {
        MeterRead read = ingestionService.ingestRead(
                dto.accountNumber, dto.readingValue, dto.readType,
                dto.readDate != null ? dto.readDate : LocalDateTime.now());
        return ResponseEntity.ok(read);
    }

    @PostMapping("/reads/batch")
    public ResponseEntity<List<MeterRead>> submitBatch(@RequestBody List<MeterReadDto> dtos) {
        List<MeterReadRequest> requests = dtos.stream()
                .map(dto -> new MeterReadRequest(
                        dto.accountNumber, dto.readingValue, dto.readType,
                        dto.readDate != null ? dto.readDate : LocalDateTime.now()))
                .toList();
        return ResponseEntity.ok(ingestionService.ingestBatch(requests));
    }

    @GetMapping("/reads/{accountId}")
    public ResponseEntity<List<MeterRead>> getReads(@PathVariable Long accountId) {
        return ResponseEntity.ok(meterReadRepo.findByAccountIdOrderByReadDateDesc(accountId));
    }

    public record MeterReadDto(
            String accountNumber,
            BigDecimal readingValue,
            MeterRead.ReadType readType,
            LocalDateTime readDate
    ) {}
}
