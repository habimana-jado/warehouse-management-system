package rw.afriteck.wms.web.inbound;


import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filters of the purchase order board. status: blank = open orders, ALL = no filter, or one PurchaseOrderStatus name. */
@Getter
@Setter
public class PurchaseOrderFilterForm {

    private String q;
    private String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderDateTo;
}
