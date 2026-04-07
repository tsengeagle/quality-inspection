package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateReviewProjectRequest;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateScoreSheetDraftRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewProjectApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProject_shouldReturn201() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setHospitalId("H001");
        req.setName("Test Project");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT", "INPATIENT"));

        mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").isNotEmpty());
    }

    @Test
    void publishVersion_newProject_shouldSucceed() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setHospitalId("H001");
        req.setName("Publish Test");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        MvcResult result = mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String projectId = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .get("data").get("projectId").asText();

        mockMvc.perform(post("/review-projects/" + projectId + "/publish-version")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void publishVersion_alreadyPublished_shouldReturn409() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setHospitalId("H001");
        req.setName("Double Publish Test");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        MvcResult result = mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String projectId = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .get("data").get("projectId").asText();

        mockMvc.perform(post("/review-projects/" + projectId + "/publish-version")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk());

        // Try to publish again — should conflict
        mockMvc.perform(post("/review-projects/" + projectId + "/publish-version")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createScoreSheetDraft_shouldReturn201() throws Exception {
        CreateScoreSheetDraftRequest req = new CreateScoreSheetDraftRequest();
        req.setProjectId("p-test");

        mockMvc.perform(post("/score-sheets")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scoreSheetId").isNotEmpty());
    }

    @Test
    void requestWithoutApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post("/review-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProject_nullMedicalRecordTypes_shouldReturn400() throws Exception {
        String body = "{\"hospitalId\":\"H001\",\"name\":\"Test\",\"medicalRecordTypes\":null}";

        mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void executeSampling_inactiveProject_shouldReturn422() throws Exception {
        // Create a DRAFT project — not published, not activated
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setHospitalId("H001");
        req.setName("T-003 Inactive Project");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        MvcResult result = mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String projectId = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .get("data").get("projectId").asText();

        // Execute sampling on DRAFT project → 422 PROJECT_NOT_ACTIVE
        mockMvc.perform(post("/review-projects/" + projectId + "/sampling/execute")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("PROJECT_NOT_ACTIVE"));
    }
}
