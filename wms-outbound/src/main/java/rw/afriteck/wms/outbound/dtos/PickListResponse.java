package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.outbound.enums.PickListStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PickListResponse(UUID id, long version, String pickNumber,
                               UUID salesOrderId, String orderNumber, UUID customerId, String customerName, UUID warehouseId,
                               PickListStatus status, UUID taskId, Instant releasedAt, Instant completedAt, String cancelReason,
                               List<PickLineResponse> lines) {}
