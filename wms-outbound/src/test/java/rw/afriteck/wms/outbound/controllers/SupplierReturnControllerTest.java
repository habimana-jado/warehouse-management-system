package rw.afriteck.wms.outbound.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.outbound.dtos.CancelSupplierReturnRequest;
import rw.afriteck.wms.outbound.dtos.CreateSupplierReturnRequest;
import rw.afriteck.wms.outbound.dtos.SupplierReturnResponse;
import rw.afriteck.wms.outbound.dtos.SupplierReturnSummaryResponse;
import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;
import rw.afriteck.wms.outbound.service.SupplierReturnService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierReturnControllerTest {

    @Mock
    private SupplierReturnService returns;
    @Mock
    private UserContext userContext;

    @InjectMocks
    private SupplierReturnController controller;

    private UUID rtvId;
    private UUID warehouseId;
    private UUID supplierId;
    private SupplierReturnResponse dummyResponse;

    @BeforeEach
    void setUp() {
        rtvId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        supplierId = UUID.randomUUID();

        dummyResponse = new SupplierReturnResponse(
                rtvId, "RTV-000001", supplierId, "Supplier 1", warehouseId, SupplierReturnStatus.DRAFT,
                LocalDate.now(), "Defective", null, null, List.of(), null, null
        );
    }

    @Test
    void create_CallsServiceAndChecksAccess() {
        CreateSupplierReturnRequest req = new CreateSupplierReturnRequest(supplierId, warehouseId, LocalDate.now(), "Defective", null, null, List.of());
        when(returns.create(req)).thenReturn(dummyResponse);

        SupplierReturnResponse res = controller.create(req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(returns).create(req);
    }

    @Test
    void search_CallsServiceAndChecksAccess() {
        when(returns.search(eq(warehouseId), any(), any(), any(Pageable.class)))
                .thenReturn(PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of())));

        PageResponse<SupplierReturnSummaryResponse> res = controller.search(warehouseId, null, "RTV", Pageable.unpaged());

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(returns).search(eq(warehouseId), any(), eq("RTV"), any(Pageable.class));
    }

    @Test
    void get_CallsServiceAndChecksAccess() {
        when(returns.warehouseIdOf(rtvId)).thenReturn(warehouseId);
        when(returns.getById(rtvId)).thenReturn(dummyResponse);

        SupplierReturnResponse res = controller.get(rtvId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(returns).getById(rtvId);
    }

    @Test
    void post_CallsServiceAndChecksAccess() {
        when(returns.warehouseIdOf(rtvId)).thenReturn(warehouseId);
        when(returns.post(rtvId)).thenReturn(dummyResponse);

        SupplierReturnResponse res = controller.post(rtvId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(returns).post(rtvId);
    }

    @Test
    void cancel_CallsServiceAndChecksAccess() {
        CancelSupplierReturnRequest req = new CancelSupplierReturnRequest("Cancelled");
        when(returns.warehouseIdOf(rtvId)).thenReturn(warehouseId);
        when(returns.cancel(rtvId, req)).thenReturn(dummyResponse);

        SupplierReturnResponse res = controller.cancel(rtvId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(returns).cancel(rtvId, req);
    }
}
