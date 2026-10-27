package rw.afriteck.wms.web.inbound;

import rw.afriteck.wms.inbound.dtos.CustomerReturnSummaryResponse;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;

import java.time.Instant;
import java.util.UUID;

/** One row of the customer return board. */
public record CustomerReturnRow(UUID id, String returnNumber, String customerName, String salesOrderNumber,
                                CustomerReturnStatus status, Instant createdAt, Instant receivedAt, int lineCount) {

    static CustomerReturnRow of(CustomerReturnSummaryResponse s) {
        return new CustomerReturnRow(s.id(), s.returnNumber(), s.customerName(), s.salesOrderNumber(),
                s.status(), s.createdAt(), s.receivedAt(), s.lineCount());
    }
}