package com.snovasys.ledgerline.controller;

import com.snovasys.ledgerline.dto.AuditEventDto;
import com.snovasys.ledgerline.dto.LedgerEntryDto;
import com.snovasys.ledgerline.repository.LedgerEntryRepository;
import com.snovasys.ledgerline.service.AuditService;
import com.snovasys.ledgerline.service.FinancialEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auditor")
@PreAuthorize("hasAnyRole('AUDITOR', 'ADMINISTRATOR')")
public class AuditorController {

    private final AuditService auditService;
    private final FinancialEngineService financialEngineService;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AuditorController(AuditService auditService, FinancialEngineService financialEngineService,
                             LedgerEntryRepository ledgerEntryRepository) {
        this.auditService = auditService;
        this.financialEngineService = financialEngineService;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @GetMapping("/audit-events")
    public ResponseEntity<List<AuditEventDto>> getAuditEvents() {
        return ResponseEntity.ok(auditService.getAllEvents());
    }

    @GetMapping("/ledger-entries")
    public ResponseEntity<List<LedgerEntryDto>> getLedgerEntries() {
        return ResponseEntity.ok(financialEngineService.getAllLedgerEntries());
    }

    @GetMapping("/ledger-integrity")
    public ResponseEntity<Map<String, Object>> getLedgerIntegrity() {
        Long totalDebits = ledgerEntryRepository.sumAmountByEntryType("DEBIT");
        Long totalCredits = ledgerEntryRepository.sumAmountByEntryType("CREDIT");
        boolean isBalanced = financialEngineService.verifyGlobalDoubleEntryInvariant();

        Map<String, Object> integrity = new HashMap<>();
        integrity.put("isBalanced", isBalanced);
        integrity.put("totalDebitsPaise", totalDebits);
        integrity.put("totalCreditsPaise", totalCredits);
        integrity.put("differencePaise", Math.abs(totalDebits - totalCredits));
        integrity.put("status", isBalanced ? "PROVEN_BALANCED" : "IMBALANCE_DETECTED");

        return ResponseEntity.ok(integrity);
    }
}
