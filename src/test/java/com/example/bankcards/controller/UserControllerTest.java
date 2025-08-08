package com.example.bankcards.controller;

import com.example.bankcards.dto.AuthResponse;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RefreshRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.security.JwtUtils;
import com.example.bankcards.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    void register_returns200() throws Exception {
        AuthResponse resp = new AuthResponse();
        resp.setAccessToken("a");
        resp.setRefreshToken("r");
        Mockito.when(userService.register(Mockito.any(RegisterRequest.class))).thenReturn(resp);

        String body = "{" +
                "\"username\":\"user01\"," +
                "\"password\":\"secret123\"," +
                "\"role\":\"USER\"}";

        mockMvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("a"));
    }

    @Test
    void register_validationError_returns400() throws Exception {
        String body = "{" +
                "\"username\":\"u\"," +
                "\"password\":\"1\"," +
                "\"role\":\"USER\"}";
        mockMvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void login_validationError_returns400() throws Exception {
        String body = "{" +
                "\"username\":\"\"," +
                "\"password\":\"\"}";
        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_validationError_returns400() throws Exception {
        String body = "{" +
                "\"refreshToken\":\"\"}";
        mockMvc.perform(post("/user/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns200() throws Exception {
        AuthResponse resp = new AuthResponse();
        resp.setAccessToken("a");
        resp.setRefreshToken("r");
        Mockito.when(userService.login(Mockito.any(LoginRequest.class))).thenReturn(resp);
        String body = "{" +
                "\"username\":\"user01\"," +
                "\"password\":\"secret123\"}";
        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("r"));
    }

    @Test
    void refresh_returns200() throws Exception {
        AuthResponse resp = new AuthResponse();
        resp.setAccessToken("a2");
        resp.setRefreshToken("r2");
        Mockito.when(userService.refresh(Mockito.any(RefreshRequest.class))).thenReturn(resp);
        String body = "{" +
                "\"refreshToken\":\"r1\"}";
        mockMvc.perform(post("/user/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("a2"));
    }
}


