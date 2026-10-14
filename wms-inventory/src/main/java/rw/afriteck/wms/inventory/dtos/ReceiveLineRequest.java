package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ReceiveLineRequest(UUID lineId, UUID toLocationId, BigDecimal quantity) {}
