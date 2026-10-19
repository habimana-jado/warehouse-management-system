package rw.afriteck.wms.inventory.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.AdjustmentReasonResponse;
import rw.afriteck.wms.inventory.dtos.CreateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.dtos.UpdateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.enums.AdjustmentDirection;
import rw.afriteck.wms.inventory.service.AdjustmentReasonService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustmentReasonControllerTest {

    @Mock
    private AdjustmentReasonService service;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private AdjustmentReasonController controller;

    private UUID reasonId;
    private AdjustmentReasonResponse response;

    @BeforeEach
    void setUp() {
        reasonId = UUID.randomUUID();
        response = new AdjustmentReasonResponse(reasonId, 1L, "DAMAGED", "Damaged", AdjustmentDirection.DECREASE, true, true);
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<AdjustmentReasonResponse> pageResponse = new PageResponse<>(List.of(response), 0, 10, 1, 1);

        when(service.search("damaged", false, pageable)).thenReturn(pageResponse);

        PageResponse<AdjustmentReasonResponse> result = controller.search("damaged", false, pageable);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        verify(service).search("damaged", false, pageable);
    }

    @Test
    void get_Success() {
        when(service.getById(reasonId)).thenReturn(response);

        AdjustmentReasonResponse result = controller.get(reasonId);

        assertNotNull(result);
        assertEquals(reasonId, result.id());
    }

    @Test
    void create_Success() {
        CreateAdjustmentReasonRequest req = new CreateAdjustmentReasonRequest("DAMAGED", "Damaged", AdjustmentDirection.DECREASE, true);
        when(service.create(req)).thenReturn(response);

        AdjustmentReasonResponse result = controller.create(req);

        assertNotNull(result);
        assertEquals("DAMAGED", result.code());
    }

    @Test
    void update_Success() {
        UpdateAdjustmentReasonRequest req = new UpdateAdjustmentReasonRequest("Damaged Goods", null, null, null);
        when(service.update(reasonId, req)).thenReturn(response);

        AdjustmentReasonResponse result = controller.update(reasonId, req);

        assertNotNull(result);
        assertEquals(reasonId, result.id());
    }
}
