package rw.afriteck.wms.web.inbound;

import lombok.Getter;
import lombok.Setter;

/** Filters of the customer return board. status: blank = expected returns, ALL = no filter, or one status name. */
@Getter
@Setter
public class ReturnFilterForm {
    private String q;
    private String status;
}