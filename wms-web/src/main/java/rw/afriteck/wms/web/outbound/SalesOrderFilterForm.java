package rw.afriteck.wms.web.outbound;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filter form of the board (GET parameters). */
@Getter
@Setter
class SalesOrderFilterForm {
    private String q;
    private String status;                                           // blank = open orders, ALL, or one status
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderDateTo;
}

