package rw.afriteck.wms.reporting.dtos;

import rw.afriteck.wms.reporting.enums.AuditSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(Instant occurredAt, AuditSource source, String event,
                                 UUID actorId, String actorName, UUID itemId, String itemSku, String itemName,
                                 UUID locationId, String locationCode, BigDecimal quantity,
                                 String referenceType, UUID referenceId, String referenceNumber, String detail) {}
