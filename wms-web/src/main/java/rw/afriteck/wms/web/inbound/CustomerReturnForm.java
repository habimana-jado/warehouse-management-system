package rw.afriteck.wms.web.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.CreateCustomerReturnRequest;
import rw.afriteck.wms.inbound.dtos.CustomerReturnLineRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class CustomerReturnForm {

    private UUID customerId;                  // optional (walk-in returns)

    @Size(max = 50)
    private String salesOrderNumber;          // plain text: outbound is another module

    @Size(max = 1000)
    private String notes;

    @Valid
    private List<ReturnLineForm> lines = new ArrayList<>(List.of(new ReturnLineForm()));

    /** itemIds: row index -> resolved item id. Blank rows are skipped. */
    CreateCustomerReturnRequest toRequest(UUID warehouseId, Map<Integer, UUID> itemIds) {
        List<CustomerReturnLineRequest> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ReturnLineForm l = lines.get(i);
            if (l.blank()) {
                continue;
            }
            out.add(new CustomerReturnLineRequest(itemIds.get(i), l.getQuantity(), Codes.blankToNull(l.getUom()),
                    Codes.blankToNull(l.getLotNumber()), l.getExpiryDate(), Codes.blankToNull(l.getSerialNumber()),
                    Codes.blankToNull(l.getReturnReason())));
        }
        return new CreateCustomerReturnRequest(warehouseId, customerId, null,
                Codes.blankToNull(salesOrderNumber), Codes.blankToNull(notes), out);
    }
}