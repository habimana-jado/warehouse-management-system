package rw.afriteck.wms.inbound.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;
import rw.afriteck.wms.inbound.service.CustomerReturnService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerReturnControllerTest {

    @Mock
    private CustomerReturnService customerReturns;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private CustomerReturnController controller;

    private UUID returnId;
    private UUID warehouseId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        returnId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private CustomerReturnResponse createSampleResponse(CustomerReturnStatus status) {
        return new CustomerReturnResponse(
                returnId, 1L, "RMA-001", warehouseId, UUID.randomUUID(), "Customer",
                UUID.randomUUID(), "SO-1", status, "Notes", null, null, null,
                null, null, Instant.now(), List.of());
    }

    @Test
    void create_Success() {
        CreateCustomerReturnRequest req = new CreateCustomerReturnRequest(
                warehouseId, UUID.randomUUID(), null, "SO-1", "Notes", List.of());
        CustomerReturnResponse expectedResponse = createSampleResponse(CustomerReturnStatus.EXPECTED);

        when(customerReturns.create(req)).thenReturn(expectedResponse);

        ResponseEntity<CustomerReturnResponse> res = controller.create(req);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(expectedResponse, res.getBody());
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<CustomerReturnSummaryResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);

        when(customerReturns.search(warehouseId, Set.of(CustomerReturnStatus.EXPECTED), "q", pageable)).thenReturn(pageResponse);

        PageResponse<CustomerReturnSummaryResponse> res = controller.search(warehouseId, Set.of(CustomerReturnStatus.EXPECTED), "q", pageable);

        assertEquals(pageResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void get_Success() {
        CustomerReturnResponse expectedResponse = createSampleResponse(CustomerReturnStatus.EXPECTED);

        when(customerReturns.getById(returnId)).thenReturn(expectedResponse);

        CustomerReturnResponse res = controller.get(returnId);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void receive_Success() {
        ReceiveCustomerReturnRequest req = new ReceiveCustomerReturnRequest("Receive notes", List.of());
        CustomerReturnResponse expectedResponse = createSampleResponse(CustomerReturnStatus.RECEIVED);

        when(customerReturns.warehouseIdOf(returnId)).thenReturn(warehouseId);
        when(userContext.currentUserId()).thenReturn(userId);
        when(customerReturns.receive(returnId, req, userId)).thenReturn(expectedResponse);

        CustomerReturnResponse res = controller.receive(returnId, req);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void cancel_Success() {
        CancelCustomerReturnRequest req = new CancelCustomerReturnRequest("Cancel reason");
        CustomerReturnResponse expectedResponse = createSampleResponse(CustomerReturnStatus.CANCELLED);

        when(customerReturns.warehouseIdOf(returnId)).thenReturn(warehouseId);
        when(customerReturns.cancel(returnId, "Cancel reason")).thenReturn(expectedResponse);

        CustomerReturnResponse res = controller.cancel(returnId, req);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }
}
