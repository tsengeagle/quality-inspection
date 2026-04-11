package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailEntity;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuditApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditTrailJpaRepository auditRepo;

    private final String hmToken = TestTokenFactory.hospitalManagerToken();
    private final String reviewerToken = TestTokenFactory.reviewerToken();

    private void seedAudit(String id, String entityType, String entityId, String action) {
        AuditTrailEntity e = new AuditTrailEntity();
        e.setId(id);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setAction(action);
        e.setActorUserId("u1");
        e.setActorUserName("Test User");
        e.setHospitalId("H001");
        e.setOccurredAt(Instant.now());
        e.setDetail("{}");
        auditRepo.save(e);
    }

    @Test
    void listAuditTrails_returns200() throws Exception {
        seedAudit("a1", "ReviewCase", "c1", "ASSIGN_REVIEWER");

        mockMvc.perform(get("/audit-trails")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listAuditTrails_reviewer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/audit-trails")
                        .header("api-key", reviewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCaseLifecycle_returns200() throws Exception {
        seedAudit("a1", "ReviewCase", "c1", "CREATED");
        seedAudit("a2", "ReviewCase", "c1", "ASSIGN_REVIEWER");

        mockMvc.perform(get("/review-cases/c1/lifecycle")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
