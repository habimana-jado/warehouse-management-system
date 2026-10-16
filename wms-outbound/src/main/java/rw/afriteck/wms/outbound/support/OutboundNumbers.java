package rw.afriteck.wms.outbound.support;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboundNumbers {
    private final EntityManager em;

    public String nextSalesOrder() { return "SO-"  + pad(next("so_number_seq")); }
    public String nextPickList()   { return "PCK-" + pad(next("pick_number_seq")); }
    public String nextShipment()   { return "DN-"  + pad(next("shipment_number_seq")); }
    public String nextReturn()     { return "RTV-" + pad(next("rtv_number_seq")); }

    private long next(String sequence) {      // constant names only, never user input; gaps are acceptable
        return ((Number) em.createNativeQuery("SELECT nextval('" + sequence + "')").getSingleResult()).longValue();
    }
    private static String pad(long n) { return String.format("%06d", n); }
}
