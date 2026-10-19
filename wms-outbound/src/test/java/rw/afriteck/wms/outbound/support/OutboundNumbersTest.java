package rw.afriteck.wms.outbound.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundNumbersTest {

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @InjectMocks
    private OutboundNumbers outboundNumbers;

    @BeforeEach
    void setUp() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
    }

    @Test
    void nextSalesOrder_FormatsCorrectly() {
        when(query.getSingleResult()).thenReturn(1L);
        assertEquals("SO-000001", outboundNumbers.nextSalesOrder());
    }

    @Test
    void nextPickList_FormatsCorrectly() {
        when(query.getSingleResult()).thenReturn(42L);
        assertEquals("PCK-000042", outboundNumbers.nextPickList());
    }

    @Test
    void nextShipment_FormatsCorrectly() {
        when(query.getSingleResult()).thenReturn(100L);
        assertEquals("DN-000100", outboundNumbers.nextShipment());
    }

    @Test
    void nextReturn_FormatsCorrectly() {
        when(query.getSingleResult()).thenReturn(999L);
        assertEquals("RTV-000999", outboundNumbers.nextReturn());
    }
}
