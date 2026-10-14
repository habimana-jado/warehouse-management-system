package rw.afriteck.wms.inventory.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostStockRequest(
        UUID transactionId,// optional: generated when null. A transfer passes the same id for its OUT and IN postings.
        Instant occurredAt,              // optional: null -> clock.instant()
        String referenceType,            // plain string, owning module's constant (normalized with Codes.normalize)
        UUID referenceId,
        String referenceNumber,
        String note,
        List<PostStockLine> lines) {}    // at least one; all lines succeed or the whole call fails
