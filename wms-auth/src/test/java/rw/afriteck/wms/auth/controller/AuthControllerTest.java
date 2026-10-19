package rw.afriteck.wms.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rw.afriteck.wms.auth.dtos.ChangePasswordRequest;
import rw.afriteck.wms.auth.dtos.LoginRequest;
import rw.afriteck.wms.auth.dtos.RefreshRequest;
import rw.afriteck.wms.auth.dtos.TokenResponse;
import rw.afriteck.wms.auth.dtos.UserResponse;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.auth.service.AuthService;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest req = new LoginRequest("testuser", "Password123!");
        UserResponse userResponse = new UserResponse(UUID.randomUUID(), "testuser", "Test User", "test@example.com", true, Set.of(RoleName.ADMIN), Set.of(), null);
        TokenResponse tokenResponse = new TokenResponse("access-token", "Bearer", 900L, "refresh-token", 604800L, false, userResponse);

        when(authService.login(any(LoginRequest.class), any())).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/wms/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refresh_Success() throws Exception {
        RefreshRequest req = new RefreshRequest("raw-refresh-token");
        UserResponse userResponse = new UserResponse(UUID.randomUUID(), "testuser", "Test User", "test@example.com", true, Set.of(RoleName.ADMIN), Set.of(), null);
        TokenResponse tokenResponse = new TokenResponse("new-access-token", "Bearer", 900L, "next-refresh-token", 604800L, false, userResponse);

        when(authService.refresh(eq("raw-refresh-token"), any())).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/wms/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void logout_Success() throws Exception {
        RefreshRequest req = new RefreshRequest("raw-refresh-token");

        mockMvc.perform(post("/api/v1/wms/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(authService).logout("raw-refresh-token");
    }

    @Test
    void logoutAll_Success() throws Exception {
        mockMvc.perform(post("/api/v1/wms/auth/logout-all"))
                .andExpect(status().isNoContent());

        verify(authService).logoutAll();
    }

    @Test
    void me_Success() throws Exception {
        UserResponse userResponse = new UserResponse(UUID.randomUUID(), "testuser", "Test User", "test@example.com", true, Set.of(RoleName.ADMIN), Set.of(), null);

        when(authService.me()).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/wms/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void changePassword_Success() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest("OldPass123!", "NewPass123!");

        mockMvc.perform(post("/api/v1/wms/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(authService).changePassword(any(ChangePasswordRequest.class));
    }
}
