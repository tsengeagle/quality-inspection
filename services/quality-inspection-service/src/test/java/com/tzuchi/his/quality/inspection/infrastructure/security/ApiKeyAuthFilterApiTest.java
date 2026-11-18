package com.tzuchi.his.quality.inspection.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 未認證一律回 401 與 {@code UNAUTHORIZED}（D-023 Consequences、api-spec §4.1）。
 * 缺少與無效的差別只寫在 message（D-061 草案 Q-5，負責人 2026-11-18 裁決：程式照 D-023）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApiKeyAuthFilterApiTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void missingApiKey_isUnauthorizedWithUnauthorizedCode() throws Exception {
        mockMvc.perform(get("/score-sheets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message", containsString("Missing api-key")));
    }

    @Test
    void invalidApiKey_isUnauthorizedWithUnauthorizedCode() throws Exception {
        mockMvc.perform(get("/score-sheets").header("api-key", "not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message", containsString("Invalid api-key")));
    }
}
