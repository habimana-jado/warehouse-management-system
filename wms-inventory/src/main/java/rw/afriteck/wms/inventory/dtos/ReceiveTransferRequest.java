package rw.afriteck.wms.inventory.dtos;

import java.util.List;

public record ReceiveTransferRequest(List<ReceiveLineRequest> lines, boolean closeShort, String shortReason) {}

