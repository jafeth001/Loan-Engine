package com.bank.loanengine.audit.controller;

import com.bank.loanengine.audit.dto.AuditLogResponse;
import com.bank.loanengine.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Audit", description = "Read-only access to the immutable audit ledger. All endpoints require ROLE_ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "List audit entries (ADMIN only)")
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "processedAt"));
        Page<AuditLogResponse> result = auditLogService.findAll(pageable).map(AuditLogResponse::from);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "List audit entries for a loan (ADMIN only)")
    @GetMapping("/loans/{loanId}")
    public ResponseEntity<Page<AuditLogResponse>> getByLoan(
            @PathVariable Long loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "processedAt"));
        Page<AuditLogResponse> result = auditLogService.findByLoanId(loanId, pageable).map(AuditLogResponse::from);
        return ResponseEntity.ok(result);
    }
}
