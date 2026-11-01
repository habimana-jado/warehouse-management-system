package rw.afriteck.wms.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rw.afriteck.wms.auth.dtos.*;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.auth.service.UserService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void search_Success() throws Exception {
        UserResponse userResponse = new UserResponse(UUID.randomUUID(), "john", "John Doe", "john@example.com", true, Set.of(RoleName.ADMIN), Set.of(), null);
        when(userService.search(eq("john"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(userResponse)));

        mockMvc.perform(get("/api/v1/wms/users?q=john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("john"));
    }

    @Test
    void create_Success() throws Exception {
        CreateUserRequest req = new CreateUserRequest("john", "John Doe", "john@example.com", "Pass123456", Set.of(RoleName.VIEWER), Set.of());
        UserResponse userResponse = new UserResponse(UUID.randomUUID(), "john", "John Doe", "john@example.com", true, Set.of(RoleName.VIEWER), Set.of(), null);

        when(userService.create(any(CreateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/wms/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void get_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(userId, "john", "John Doe", "john@example.com", true, Set.of(RoleName.ADMIN), Set.of(), null);

        when(userService.get(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/wms/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void update_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest req = new UpdateUserRequest("John Updated", "john.updated@example.com");
        UserResponse userResponse = new UserResponse(userId, "john", "John Updated", "john.updated@example.com", true, Set.of(RoleName.ADMIN), Set.of(), null);

        when(userService.update(eq(userId), any(UpdateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/wms/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Updated"));
    }

    @Test
    void roles_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        AssignRolesRequest req = new AssignRolesRequest(Set.of(RoleName.PICKER));
        UserResponse userResponse = new UserResponse(userId, "john", "John Doe", "john@example.com", true, Set.of(RoleName.PICKER), Set.of(), null);

        when(userService.assignRoles(eq(userId), any(AssignRolesRequest.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/wms/users/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("PICKER"));
    }

    @Test
    void warehouses_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID whId = UUID.randomUUID();
        AssignWarehousesRequest req = new AssignWarehousesRequest(Set.of(whId));
        UserResponse userResponse = new UserResponse(userId, "john", "John Doe", "john@example.com", true, Set.of(RoleName.PICKER), Set.of(whId), null);

        when(userService.assignWarehouses(eq(userId), any(AssignWarehousesRequest.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/wms/users/{id}/warehouses", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseIds[0]").value(whId.toString()));
    }

    @Test
    void activate_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(userId, "john", "John Doe", "john@example.com", true, Set.of(RoleName.ADMIN), Set.of(), null);

        when(userService.setActive(userId, true)).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/wms/users/{id}/activate", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivate_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(userId, "john", "John Doe", "john@example.com", false, Set.of(RoleName.ADMIN), Set.of(), null);

        when(userService.setActive(userId, false)).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/wms/users/{id}/deactivate", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void resetPassword_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        ResetPasswordRequest req = new ResetPasswordRequest("NewPass123!");

        mockMvc.perform(post("/api/v1/wms/users/{id}/reset-password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(userService).resetPassword(eq(userId), any(ResetPasswordRequest.class));
    }
}
