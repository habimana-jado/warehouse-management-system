package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;

public record ConfirmTransferLineRequest(BigDecimal quantity, String shortReason) {}   // 0..planned; below planned needs shortReason
