package rw.afriteck.wms.inventory.dtos;

import java.util.List;
import java.util.UUID;

public record PostStockResponse(
        UUID transactionId,
        List<MovementResponse> movements) {}

