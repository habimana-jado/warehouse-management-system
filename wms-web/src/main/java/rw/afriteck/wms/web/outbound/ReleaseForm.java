package rw.afriteck.wms.web.outbound;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.outbound.dtos.ReleaseSalesOrderRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Getter
@Setter
class ReleaseForm {
    private TaskPriority priority = TaskPriority.NORMAL;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;                                     // empty = end of the order's required date (service default)
    @Size(max = 1000)
    private String notes;

    /**
     * The due date is inclusive in the UI: the instant is the end of that day in the UI zone.
     */
    ReleaseSalesOrderRequest toRequest(ZoneId zone) {
        Instant dueAt = dueDate == null ? null : dueDate.plusDays(1).atStartOfDay(zone).toInstant();
        return new ReleaseSalesOrderRequest(priority, dueAt, Codes.blankToNull(notes));
    }
}
