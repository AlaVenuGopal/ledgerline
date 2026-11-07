package com.snovasys.ledgerline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snovasys.ledgerline.dto.LoginRequest;
import com.snovasys.ledgerline.dto.TransferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RoleAuthorizationSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String getJwtToken(String username, String password) throws Exception {
        LoginRequest login = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("Account holder is denied access to admin endpoints (403 Forbidden)")
    void accountHolderDeniedAdminEndpoint() throws Exception {
        String token = getJwtToken("alice", "AlicePassword@123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Auditor is denied initiating financial transfers (403 Forbidden)")
    void auditorDeniedTransferExecution() throws Exception {
        String token = getJwtToken("auditor", "AuditorPassword@123");

        TransferRequest transferRequest = new TransferRequest(2L, 3L, 1000L, "Test by auditor", null);

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrator can access admin users list (200 OK)")
    void adminCanAccessAdminUsers() throws Exception {
        String token = getJwtToken("admin", "AdminPassword@123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Auditor can access read-only audit records (200 OK)")
    void auditorCanAccessAuditLogs() throws Exception {
        String token = getJwtToken("auditor", "AuditorPassword@123");

        mockMvc.perform(get("/api/auditor/audit-events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
