package com.raiffeisen.socks.controller;

import com.raiffeisen.socks.repository.SocksRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты: MockMvc + Testcontainers (реальный PostgreSQL).
 * Пропускаются, если Docker недоступен.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SocksControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocksRepository socksRepository;

    @BeforeEach
    void cleanDb() {
        socksRepository.deleteAllInBatch();
    }

    @Test
    void income_success_returns200() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":80,\"quantity\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void income_invalidCottonPart_returns400() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":150,\"quantity\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void income_invalidQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":80,\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void income_missingColor_returns400() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cottonPart\":80,\"quantity\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outcome_success_returns200() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"black\",\"cottonPart\":50,\"quantity\":100}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/socks/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"black\",\"cottonPart\":50,\"quantity\":30}"))
                .andExpect(status().isOk());
    }

    @Test
    void outcome_insufficient_returns400() throws Exception {
        mockMvc.perform(post("/api/socks/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"white\",\"cottonPart\":10,\"quantity\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_moreThan_returnsSum() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":95,\"quantity\":10}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":80,\"quantity\":20}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/socks")
                        .param("color", "red")
                        .param("operation", "moreThan")
                        .param("cottonPart", "90"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("10", result.getResponse().getContentAsString());
    }

    @Test
    void get_equal_returnsSum() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":80,\"quantity\":10}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":90,\"quantity\":5}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/socks")
                        .param("color", "red")
                        .param("operation", "equal")
                        .param("cottonPart", "80"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("10", result.getResponse().getContentAsString());
    }

    @Test
    void get_lessThan_returnsSum() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":80,\"quantity\":10}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":90,\"quantity\":5}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/socks")
                        .param("color", "red")
                        .param("operation", "lessThan")
                        .param("cottonPart", "85"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("10", result.getResponse().getContentAsString());
    }

    @Test
    void get_invalidOperation_returns400() throws Exception {
        mockMvc.perform(get("/api/socks")
                        .param("color", "red")
                        .param("operation", "wrongOp")
                        .param("cottonPart", "50"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_missingParams_returns400() throws Exception {
        mockMvc.perform(get("/api/socks")
                        .param("color", "red"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_unknownPath_returns404() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_unknownApiPath_returns404() throws Exception {
        mockMvc.perform(get("/api/socks/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_wrongMethod_returns405() throws Exception {
        mockMvc.perform(post("/api/socks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":80,\"quantity\":10}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void income_brokenJson_returns400() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void income_wrongContentType_returns400() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_nonNumericCottonPart_returns400() throws Exception {
        mockMvc.perform(get("/api/socks")
                        .param("color", "red")
                        .param("operation", "equal")
                        .param("cottonPart", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void income_missingCottonPart_returns400() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"quantity\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void income_missingQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":80}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void income_colorTooLong_returns400() throws Exception {
        String longColor = "a".repeat(60);
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"" + longColor + "\",\"cottonPart\":80,\"quantity\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void income_sameColorTwice_accumulates() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":80,\"quantity\":10}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"red\",\"cottonPart\":80,\"quantity\":10}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/socks")
                        .param("color", "red")
                        .param("operation", "equal")
                        .param("cottonPart", "80"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("20", result.getResponse().getContentAsString());
    }

    @Test
    void outcome_fullDrain_returnsZero() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"blue\",\"cottonPart\":50,\"quantity\":50}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/socks/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"blue\",\"cottonPart\":50,\"quantity\":50}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/socks")
                        .param("color", "blue")
                        .param("operation", "equal")
                        .param("cottonPart", "50"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("0", result.getResponse().getContentAsString());
    }

    @Test
    void income_colorCaseInsensitive() throws Exception {
        mockMvc.perform(post("/api/socks/income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"Red\",\"cottonPart\":20,\"quantity\":10}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/socks")
                        .param("color", "red")
                        .param("operation", "equal")
                        .param("cottonPart", "20"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("10", result.getResponse().getContentAsString());
    }
}
