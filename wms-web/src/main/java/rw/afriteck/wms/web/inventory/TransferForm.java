package rw.afriteck.wms.web.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.CreateTransferLineRequest;
import rw.afriteck.wms.inventory.dtos.CreateTransferRequest;
import rw.afriteck.wms.inventory.enums.TransferType;
import rw.afriteck.wms.operations.enums.TaskPriority;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TransferForm {

    @NotNull
    private TransferMode mode = TransferMode.MOVE_NOW;
    private UUID destinationWarehouseId;                        // INTER_WAREHOUSE only
    private TaskPriority priority;                              // planned and inter-warehouse; blank = normal
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate dueDate;
    @Size(max = 500) private String notes;
    @Valid
    @NotEmpty
    private List<TransferLineForm> lines = new ArrayList<>();

    /** Read as properties by the templates: form.inter, form.moveNow. */
    public boolean isInter()   { return mode == TransferMode.INTER_WAREHOUSE; }
    public boolean isMoveNow() { return mode == TransferMode.MOVE_NOW; }

    CreateTransferRequest toRequest(UUID sourceWarehouseId, List<UUID> itemIds, ZoneId zone) {
        List<CreateTransferLineRequest> out = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            out.add(lines.get(i).toRequest(itemIds.get(i), isInter()));
        }
        Instant dueAt = (dueDate == null || isMoveNow()) ? null : dueDate.plusDays(1).atStartOfDay(zone).toInstant();  // end of the due day
        return new CreateTransferRequest(
                isInter() ? TransferType.INTER_WAREHOUSE : TransferType.BIN,
                sourceWarehouseId,
                isInter() ? destinationWarehouseId : null,
                isMoveNow(),
                isMoveNow() ? null : priority,
                dueAt,
                Codes.blankToNull(notes),
                out);
    }
}
