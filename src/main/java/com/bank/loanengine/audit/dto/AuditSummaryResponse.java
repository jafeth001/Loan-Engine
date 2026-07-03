package com.bank.loanengine.audit.dto;

import com.bank.loanengine.audit.domain.AuditEventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Aggregate count of audit entries grouped by event type")
public record AuditSummaryResponse(

        @Schema(description = "Total number of audit entries across all event types", example = "58")
        long totalEntries,

        @Schema(description = "Breakdown of entry counts per event type",
                example = "{\"LOAN_CREATED\": 12, \"PREPAYMENT_APPLIED\": 46}")
        Map<AuditEventType, Long> countByEventType
) {}
