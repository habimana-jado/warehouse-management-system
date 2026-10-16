package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.reporting.dtos.AuditFeedResponse;
import rw.afriteck.wms.reporting.enums.AuditSource;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public interface AuditReportService {
    AuditFeedResponse feed(UUID warehouseId, LocalDate from, LocalDate to, Set<AuditSource> sources,
                           UUID actorId, int page, int size);
}
