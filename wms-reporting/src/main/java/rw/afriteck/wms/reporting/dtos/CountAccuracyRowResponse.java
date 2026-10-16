package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CountAccuracyRowResponse(UUID countId, String countNumber, UUID zoneLocationId, String zoneCode,
                                       Instant completedAt, UUID approvedBy, String approvedByName,
                                       long lines, long matchingLines, long overLines, long underLines,
                                       BigDecimal netVariance, BigDecimal absVariance, BigDecimal accuracy) {}
