package rw.afriteck.wms.web.inventory;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.inventory.enums.MovementType;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class MovementFilterForm {
    private UUID locationId;
    private String item;
    private String serial;
    private MovementType type;                                   // blank = all types
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate from;   // inclusive in the UI
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate to;     // inclusive in the UI
}
