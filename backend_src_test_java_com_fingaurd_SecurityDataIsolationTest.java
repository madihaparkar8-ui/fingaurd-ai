package com.fingaurd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fingaurd.dto.request.RegisterRequest;
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
public class SecurityDataIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("User A cannot access User B's journey endpoint")
    void testUserDataIsolation() throws Exception {
        // Register User A
        String emailA = "usera_" + System.currentTimeMillis() + "@fingaurd.ai";
        MvcResult resA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("User A", emailA, "Password123"))))
                .andExpect(status().isOk())
                .andReturn();
        String tokenA = objectMapper.readTree(resA.getResponse().getContentAsString()).get("token").asText();

        // Register User B
        String emailB = "userb_" + System.currentTimeMillis() + "@fingaurd.ai";
        MvcResult resB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("User B", emailB, "Password123"))))
                .andExpect(status().isOk())
                .andReturn();
        Long userIdB = objectMapper.readTree(resB.getResponse().getContentAsString()).get("userId").asLong();

        // User A attempts to access User B's journey URL: /api/users/{userIdB}/journey -> must be rejected (401 UnauthorizedException)
        mockMvc.perform(get("/api/users/" + userIdB + "/journey")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnauthorized());
    }
}
