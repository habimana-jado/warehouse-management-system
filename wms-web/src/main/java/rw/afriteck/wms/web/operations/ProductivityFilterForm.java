package rw.afriteck.wms.web.operations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.operations.enums.TaskType;

import java.time.LocalDate;

@Getter @Setter
public class ProductivityFilterForm {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate from;   // inclusive
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate to;     // inclusive in the UI
    private TaskType type;
}
