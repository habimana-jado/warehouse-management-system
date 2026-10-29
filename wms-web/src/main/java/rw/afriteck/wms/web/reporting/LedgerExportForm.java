package rw.afriteck.wms.web.reporting;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.inventory.enums.MovementType;

import java.time.LocalDate;

@Getter @Setter
public class LedgerExportForm {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate to;       // inclusive in the UI
    private MovementType type;             // blank = all
}
