package rw.afriteck.wms.reporting.dtos;

import java.util.List;

public record AuditFeedResponse(List<AuditEventResponse> content, int page, int size, boolean hasMore) {}
