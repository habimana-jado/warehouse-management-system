package rw.afriteck.wms.web.inbound;

import rw.afriteck.wms.inbound.dtos.CustomerReturnResponse;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;
import rw.afriteck.wms.web.inventory.BinOptions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Everything the customer return detail shows. lineInfo is keyed by return line id; the bin lists are only filled when the user can receive. */
public record CustomerReturnView(CustomerReturnResponse ret, boolean canManage, boolean canReceive,
                                 Map<UUID, ReturnLineInfo> lineInfo,
                                 List<BinOptions.BinOption> storageBins,
                                 List<BinOptions.BinOption> damagedBins) {

    public boolean canReceiveGoods() {
        return canReceive && ret.status() == CustomerReturnStatus.EXPECTED;
    }

    public boolean canCancel() {
        return canManage && ret.status() == CustomerReturnStatus.EXPECTED;
    }
}