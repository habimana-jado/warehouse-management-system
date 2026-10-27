package rw.afriteck.wms.web.outbound;

import rw.afriteck.wms.outbound.dtos.PickLineResponse;

import java.util.List;

record PickLineView(PickLineResponse line, String tracking, boolean canWork, boolean quickPick,
                    PickConfirmForm form, List<StockOption> stock, int stockMore, String error) {
}
