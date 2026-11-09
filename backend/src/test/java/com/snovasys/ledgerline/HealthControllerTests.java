package com.snovasys.ledgerline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snovasys.ledgerline.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HealthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/health returns 200 OK and status UP")
    void healthCheckReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("LedgerLine Core Engine"));
    }

    @Test
    @DisplayName("GET /health also returns 200 OK for platform probes")
    void rootHealthCheckReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET / returns 200 OK for root service verification")
    void rootEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("OPTIONS preflight from Render frontend origin returns 200 with CORS headers")
    void corsPreflightFromRenderFrontendAllowed() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://ledgerline-frontend-7fo1.onrender.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://ledgerline-frontend-7fo1.onrender.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login from Render frontend origin includes CORS headers")
    void loginWithCorsFromRenderFrontendOrigin() throws Exception {
        LoginRequest request = new LoginRequest("alice", "AlicePassword@123");

        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "https://ledgerline-frontend-7fo1.onrender.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://ledgerline-frontend-7fo1.onrender.com"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value("alice"));
    }
}
