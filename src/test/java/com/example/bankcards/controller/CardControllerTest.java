package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.service.CardService;
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

@WebMvcTest(controllers = CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    void create_returns201AndBody() throws Exception {
        CardResponse resp = new CardResponse();
        resp.setId(1L);
        resp.setCardNumber("**** **** **** 5678");
        resp.setOwner("user1");
        resp.setStatus("ACTIVE");
        resp.setBalance(1000L);
        Mockito.when(cardService.createCard(Mockito.any(CardDTO.class))).thenReturn(resp);

        String body = "{" +
                "\"number\":\"1234567812345678\"," +
                "\"ownerId\":1," +
                "\"expiration\":1893456000000," +
                "\"balance\":1000}";

        mockMvc.perform(post("/card").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardNumber").value("**** **** **** 5678"));
    }

    @Test
    void read_returns200AndPage() throws Exception {
        CardResponse resp = new CardResponse();
        resp.setId(1L);
        resp.setCardNumber("**** **** **** 5678");
        Page<CardResponse> page = new PageImpl<>(List.of(resp), PageRequest.of(0, 10), 1);
        Mockito.when(cardService.getCards(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(page);

        mockMvc.perform(get("/card").param("page", "0").param("size", "10").principal(() -> "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getById_returns200() throws Exception {
        CardResponse resp = new CardResponse();
        resp.setId(10L);
        resp.setCardNumber("**** **** **** 1234");
        Mockito.when(cardService.getCardById(Mockito.eq(10L), Mockito.anyString())).thenReturn(resp);

        mockMvc.perform(get("/card/10").principal(() -> "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void update_returns200() throws Exception {
        CardResponse resp = new CardResponse();
        resp.setId(10L);
        resp.setCardNumber("**** **** **** 4321");
        Mockito.when(cardService.updateCard(Mockito.eq(10L), Mockito.any(CardDTO.class))).thenReturn(resp);

        String body = "{" +
                "\"number\":\"1234567812345678\"," +
                "\"ownerId\":1," +
                "\"expiration\":1893456000000," +
                "\"balance\":2000}";

        mockMvc.perform(patch("/card/10").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/card/10"))
                .andExpect(status().isNoContent());
        Mockito.verify(cardService).deleteCard(10L);
    }

    @Test
    void block_returns200() throws Exception {
        mockMvc.perform(post("/card/10/block").principal(() -> "user1"))
                .andExpect(status().isOk());
        Mockito.verify(cardService).requestBlock(10L, "user1");
    }

    @Test
    void blockAdmin_returns200() throws Exception {
        mockMvc.perform(post("/card/10/block-admin"))
                .andExpect(status().isOk());
        Mockito.verify(cardService).blockCardAdmin(10L);
    }

    @Test
    void activateAdmin_returns200() throws Exception {
        mockMvc.perform(post("/card/10/activate"))
                .andExpect(status().isOk());
        Mockito.verify(cardService).activateCardAdmin(10L);
    }

    @Test
    void transfer_returns200() throws Exception {
        String body = "{" +
                "\"fromCardId\":10," +
                "\"toCardId\":20," +
                "\"amount\":100}";
        mockMvc.perform(post("/card/transfer").contentType(MediaType.APPLICATION_JSON).content(body).principal(() -> "user1"))
                .andExpect(status().isOk());
        Mockito.verify(cardService).transfer(Mockito.any(TransferDTO.class), Mockito.eq("user1"));
    }

    @Test
    void create_validationError_returns400() throws Exception {
        // ownerId отсутствует
        String body = "{" +
                "\"number\":\"1234567812345678\"," +
                "\"expiration\":1893456000000," +
                "\"balance\":1000}";
        mockMvc.perform(post("/card").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void transfer_validationError_returns400() throws Exception {
        // amount отсутствует
        String body = "{" +
                "\"fromCardId\":10," +
                "\"toCardId\":20}";
        mockMvc.perform(post("/card/transfer").contentType(MediaType.APPLICATION_JSON).content(body).principal(() -> "user1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getById_forbidden_maps403() throws Exception {
        Mockito.when(cardService.getCardById(Mockito.eq(99L), Mockito.anyString()))
                .thenThrow(new SecurityException("Access denied: Card does not belong to user"));

        mockMvc.perform(get("/card/99").principal(() -> "user1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    void getById_badRequest_maps400() throws Exception {
        Mockito.when(cardService.getCardById(Mockito.eq(1000L), Mockito.anyString()))
                .thenThrow(new IllegalArgumentException("Card not found: 1000"));

        mockMvc.perform(get("/card/1000").principal(() -> "user1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Card not found")));
    }
}


