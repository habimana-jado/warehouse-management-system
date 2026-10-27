package rw.afriteck.wms.web.inbound;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Backs the inline "tolerance" form on a PO line: one number between 0 and 100. */
@Getter
@Setter
public class ToleranceForm {

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal tolerancePercent;
}