package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.dto.UserUpdateDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.service.UserAdminService;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
public class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAdminService service;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    void list_returns200() throws Exception {
        User u = new User();
        u.setId(1L);
        u.setUsername("u1");
        u.setRole(User.Role.USER);
        Page<User> page = new PageImpl<>(List.of(u), PageRequest.of(0, 10), 1);
        Mockito.when(service.list(Mockito.any(), Mockito.any())).thenReturn(page);

        mockMvc.perform(get("/admin/users").param("page","0").param("size","10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("u1"));
    }

    @Test
    void create_returns201() throws Exception {
        User u = new User();
        u.setId(10L);
        u.setUsername("user02");
        u.setRole(User.Role.USER);
        Mockito.when(service.create(Mockito.any(UserDTO.class))).thenReturn(u);

        String body = "{" +
                "\"username\":\"user02\"," +
                "\"password\":\"secret123\"," +
                "\"role\":\"USER\"}";

        mockMvc.perform(post("/admin/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user02"));
    }

    @Test
    void create_validationError_returns400() throws Exception {
        // username слишком короткий
        String body = "{" +
                "\"username\":\"u\"," +
                "\"password\":\"123\"," +
                "\"role\":\"USER\"}";
        mockMvc.perform(post("/admin/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void update_validationError_returns400() throws Exception {
        // username слишком короткий
        String body = "{" +
                "\"username\":\"u\"}";
        mockMvc.perform(patch("/admin/users/10").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void delete_notFound_returns400() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("User not found: 404")).when(service).delete(404L);
        mockMvc.perform(delete("/admin/users/404"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    void update_returns200() throws Exception {
        User u = new User();
        u.setId(10L);
        u.setUsername("user03");
        u.setRole(User.Role.ADMIN);
        Mockito.when(service.update(Mockito.eq(10L), Mockito.any(UserUpdateDTO.class))).thenReturn(u);

        String body = "{" +
                "\"username\":\"user03\"," +
                "\"role\":\"ADMIN\"}";

        mockMvc.perform(patch("/admin/users/10").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/admin/users/10"))
                .andExpect(status().isNoContent());
        Mockito.verify(service).delete(10L);
    }
}


