package rw.afriteck.wms.web.inventory;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.inventory.dtos.StartCountRequest;
import rw.afriteck.wms.operations.enums.TaskPriority;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Getter @Setter
public class StartCountForm {
    private TaskPriority priority;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate dueDate;

    StartCountRequest toRequest(ZoneId zone) {
        Instant dueAt = dueDate == null ? null : dueDate.plusDays(1).atStartOfDay(zone).toInstant();   // end of the due day
        return new StartCountRequest(priority, dueAt);
    }
}
