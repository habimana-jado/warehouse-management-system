package rw.afriteck.wms.inventory.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.inventory.dtos.AdjustmentReasonResponse;
import rw.afriteck.wms.inventory.dtos.CreateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.dtos.UpdateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.entity.AdjustmentReason;
import rw.afriteck.wms.inventory.enums.AdjustmentDirection;
import rw.afriteck.wms.inventory.repository.AdjustmentReasonRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustmentReasonServiceImplTest {

    @Mock
    private AdjustmentReasonRepository reasons;

    @InjectMocks
    private AdjustmentReasonServiceImpl service;

    private UUID reasonId;
    private AdjustmentReason reason;

    @BeforeEach
    void setUp() {
        reasonId = UUID.randomUUID();
        reason = new AdjustmentReason();
        reason.setId(reasonId);
        reason.setCode("DAMAGED");
        reason.setName("Damaged Goods");
        reason.setDirection(AdjustmentDirection.DECREASE);
        reason.setRequiresApproval(true);
        reason.setActive(true);
    }

    @Test
    void create_Success() {
        CreateAdjustmentReasonRequest req = new CreateAdjustmentReasonRequest("DAMAGED", "Damaged Goods", AdjustmentDirection.DECREASE, true);
        when(reasons.existsByCode("DAMAGED")).thenReturn(false);
        when(reasons.saveAndFlush(any(AdjustmentReason.class))).thenReturn(reason);

        AdjustmentReasonResponse res = service.create(req);

        assertNotNull(res);
        assertEquals("DAMAGED", res.code());
        assertEquals("Damaged Goods", res.name());
        assertEquals(AdjustmentDirection.DECREASE, res.direction());
        assertTrue(res.requiresApproval());
    }

    @Test
    void create_MissingCodeOrName_ThrowsBusinessException() {
        CreateAdjustmentReasonRequest reqNullCode = new CreateAdjustmentReasonRequest(null, "Damaged Goods", AdjustmentDirection.DECREASE, true);
        BusinessException ex1 = assertThrows(BusinessException.class, () -> service.create(reqNullCode));
        assertEquals("REASON_FIELDS_REQUIRED", ex1.getCode());

        CreateAdjustmentReasonRequest reqBlankName = new CreateAdjustmentReasonRequest("DAMAGED", "   ", AdjustmentDirection.DECREASE, true);
        BusinessException ex2 = assertThrows(BusinessException.class, () -> service.create(reqBlankName));
        assertEquals("REASON_FIELDS_REQUIRED", ex2.getCode());
    }

    @Test
    void create_DuplicateCode_ThrowsConflictException() {
        CreateAdjustmentReasonRequest req = new CreateAdjustmentReasonRequest("DAMAGED", "Damaged Goods", AdjustmentDirection.DECREASE, true);
        when(reasons.existsByCode("DAMAGED")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(req));
        assertEquals("REASON_CODE_EXISTS", ex.getCode());
    }

    @Test
    void update_Success() {
        UpdateAdjustmentReasonRequest req = new UpdateAdjustmentReasonRequest("Updated Name", AdjustmentDirection.BOTH, false, false);
        when(reasons.findById(reasonId)).thenReturn(Optional.of(reason));
        when(reasons.saveAndFlush(any(AdjustmentReason.class))).thenReturn(reason);

        AdjustmentReasonResponse res = service.update(reasonId, req);

        assertNotNull(res);
        verify(reasons).saveAndFlush(reason);
        assertEquals("Updated Name", reason.getName());
        assertEquals(AdjustmentDirection.BOTH, reason.getDirection());
        assertFalse(reason.isRequiresApproval());
        assertFalse(reason.isActive());
    }

    @Test
    void update_NotFound_ThrowsNotFoundException() {
        UpdateAdjustmentReasonRequest req = new UpdateAdjustmentReasonRequest("Updated Name", null, null, null);
        when(reasons.findById(reasonId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(reasonId, req));
    }

    @Test
    void update_BlankName_ThrowsBusinessException() {
        UpdateAdjustmentReasonRequest req = new UpdateAdjustmentReasonRequest("  ", null, null, null);
        when(reasons.findById(reasonId)).thenReturn(Optional.of(reason));

        BusinessException exActual = assertThrows(BusinessException.class, () -> service.update(reasonId, req));
        assertEquals("REASON_FIELDS_REQUIRED", exActual.getCode());
    }

    @Test
    void getById_Success() {
        when(reasons.findById(reasonId)).thenReturn(Optional.of(reason));

        AdjustmentReasonResponse res = service.getById(reasonId);

        assertNotNull(res);
        assertEquals(reasonId, res.id());
        assertEquals("DAMAGED", res.code());
    }

    @Test
    void getById_NotFound_ThrowsNotFoundException() {
        when(reasons.findById(reasonId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(reasonId));
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AdjustmentReason> page = new PageImpl<>(List.of(reason), pageable, 1);
        when(reasons.search(any(), eq(true), any(Pageable.class))).thenReturn(page);

        PageResponse<AdjustmentReasonResponse> res = service.search("damage", true, pageable);

        assertNotNull(res);
        assertEquals(1, res.totalElements());
        assertEquals("DAMAGED", res.content().get(0).code());
    }
}
