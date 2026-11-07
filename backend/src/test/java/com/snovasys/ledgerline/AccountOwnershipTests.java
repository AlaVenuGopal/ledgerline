package com.snovasys.ledgerline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snovasys.ledgerline.dto.LoginRequest;
import com.snovasys.ledgerline.dto.TransferRequest;
import com.snovasys.ledgerline.dto.WithdrawRequest;
import com.snovasys.ledgerline.entity.Account;
import com.snovasys.ledgerline.repository.AccountRepository;
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
public class AccountOwnershipTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

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
    @DisplayName("Alice cannot withdraw funds from Bob's account (403 Forbidden)")
    void aliceCannotWithdrawFromBobsAccount() throws Exception {
        String aliceToken = getJwtToken("alice", "AlicePassword@123");
        Account bobAccount = accountRepository.findByAccountNumber("LL-ACC-BOB-01").orElseThrow();

        WithdrawRequest request = new WithdrawRequest(bobAccount.getId(), 50000L, "Illicit withdrawal attempt", null);

        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Alice cannot initiate transfer out of Bob's account (403 Forbidden)")
    void aliceCannotTransferOutOfBobsAccount() throws Exception {
        String aliceToken = getJwtToken("alice", "AlicePassword@123");
        Account aliceAccount = accountRepository.findByAccountNumber("LL-ACC-ALICE-01").orElseThrow();
        Account bobAccount = accountRepository.findByAccountNumber("LL-ACC-BOB-01").orElseThrow();

        TransferRequest request = new TransferRequest(bobAccount.getId(), aliceAccount.getId(), 50000L, "Theft attempt", null);

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Alice cannot view Bob's account statement (403 Forbidden)")
    void aliceCannotViewBobsStatement() throws Exception {
        String aliceToken = getJwtToken("alice", "AlicePassword@123");
        Account bobAccount = accountRepository.findByAccountNumber("LL-ACC-BOB-01").orElseThrow();

        mockMvc.perform(get("/api/accounts/" + bobAccount.getId() + "/statement")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
    }
}
