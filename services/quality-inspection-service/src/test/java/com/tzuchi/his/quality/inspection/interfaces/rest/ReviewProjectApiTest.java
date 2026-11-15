package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewProjectApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE = "/review-projects";
    private String hmToken = TestTokenFactory.hospitalManagerToken();
    private String saToken = TestTokenFactory.systemAdminToken();

    // --- CREATE ---

    @Test
    void createProject_shouldReturn201() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setName("Test Project");
        req.setDescription("A test description");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT", "INPATIENT"));

        mockMvc.perform(post(BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.projectId").isNotEmpty());
    }

    @Test
    void createProject_withScoringInputs_shouldReturn201() throws Exception {
        String body = "{\"name\":\"門診\",\"medicalRecordTypes\":[\"OUTPATIENT\"],"
                + "\"qualityThreshold\":7,\"initialReviewWeight\":0.7,\"rereviewWeight\":0.3}";

        mockMvc.perform(post(BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createProject_qualityThresholdOutOfRange_shouldReturn400ValidationError() throws Exception {
        // D-046: 品質線是 1 到 10 的整數，超出範圍或不是整數回 400 VALIDATION_ERROR
        for (String threshold : new String[] {"11", "0", "5.5"}) {
            String body = "{\"name\":\"門診\",\"medicalRecordTypes\":[\"OUTPATIENT\"],\"qualityThreshold\":" + threshold + "}";
            mockMvc.perform(post(BASE)
                            .header("api-key", hmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void createProject_withoutMedicalRecordTypes_shouldReturn400ValidationError() throws Exception {
        // D-040: 病歷類型至少一個；沒給或空集合都是 400 VALIDATION_ERROR
        for (String body : new String[] {"{\"name\":\"門診\",\"medicalRecordTypes\":[]}", "{\"name\":\"門診\"}"}) {
            mockMvc.perform(post(BASE)
                            .header("api-key", hmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void createProject_nullName_shouldReturn400() throws Exception {
        String body = "{\"medicalRecordTypes\":[\"OUTPATIENT\"]}";

        mockMvc.perform(post(BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProject_systemAdmin_shouldReturn403() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setName("Test");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        mockMvc.perform(post(BASE)
                        .header("api-key", saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // --- PUBLISH ---

    @Test
    void publishVersion_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Publish Test");

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void publishVersion_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(post(BASE + "/no-such/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishVersion_alreadyPublished_shouldReturn409() throws Exception {
        String projectId = createProjectAndGetId("Double Publish");

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isConflict());
    }

    // --- UPDATE DRAFT ---

    @Test
    void updateDraft_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Before Update");

        UpdateReviewProjectRequest req = new UpdateReviewProjectRequest();
        req.setName("After Update");
        req.setDescription("Updated desc");
        req.setMedicalRecordTypes(Set.of("INPATIENT"));

        mockMvc.perform(put(BASE + "/" + projectId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void updateDraft_activeProject_shouldReturn409() throws Exception {
        String projectId = createActiveProject();

        UpdateReviewProjectRequest req = new UpdateReviewProjectRequest();
        req.setName("Changed");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        mockMvc.perform(put(BASE + "/" + projectId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- BIND SCORE SHEET ---

    @Test
    void bindScoreSheet_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Bind Test");

        BindScoreSheetRequest req = new BindScoreSheetRequest();
        req.setScoreSheetId("ss-123");

        mockMvc.perform(post(BASE + "/" + projectId + "/bind-score-sheet")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // --- ACTIVATE ---

    @Test
    void activate_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Activate Test");

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        BindScoreSheetRequest bindReq = new BindScoreSheetRequest();
        bindReq.setScoreSheetId("ss-1");
        mockMvc.perform(post(BASE + "/" + projectId + "/bind-score-sheet")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bindReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/" + projectId + "/activate")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void activate_noBoundScoreSheet_shouldReturn422() throws Exception {
        String projectId = createProjectAndGetId("No SS");

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/" + projectId + "/activate")
                        .header("api-key", hmToken))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- DEACTIVATE ---

    @Test
    void deactivate_shouldReturn200() throws Exception {
        String projectId = createActiveProject();

        mockMvc.perform(post(BASE + "/" + projectId + "/deactivate")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void deactivate_draftProject_shouldReturn409() throws Exception {
        String projectId = createProjectAndGetId("Draft Only");

        mockMvc.perform(post(BASE + "/" + projectId + "/deactivate")
                        .header("api-key", hmToken))
                .andExpect(status().isConflict());
    }

    // --- LIST ---

    @Test
    void listProjects_hospitalManager_shouldReturnOwnOnly() throws Exception {
        createProjectAndGetId("HM Project");

        mockMvc.perform(get(BASE)
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("HM Project"));
    }

    @Test
    void listProjects_systemAdmin_shouldReturnAll() throws Exception {
        createProjectAndGetId("SA Project");

        mockMvc.perform(get(BASE)
                        .header("api-key", saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // --- GET DETAIL ---

    @Test
    void getProject_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Detail Test");

        mockMvc.perform(get(BASE + "/" + projectId)
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.name").value("Detail Test"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void getProject_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE + "/no-such")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }

    // --- GET VERSION ---

    @Test
    void getVersion_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Version Test");

        mockMvc.perform(get(BASE + "/" + projectId + "/version")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNumber").value(1))
                .andExpect(jsonPath("$.data.published").value(false));
    }

    // --- Helpers ---

    private String createProjectAndGetId(String name) throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setName(name);
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        MvcResult result = mockMvc.perform(post(BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return (String) data.get("projectId");
    }

    private String createActiveProject() throws Exception {
        String projectId = createProjectAndGetId("Active Project");

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        BindScoreSheetRequest bindReq = new BindScoreSheetRequest();
        bindReq.setScoreSheetId("ss-1");
        mockMvc.perform(post(BASE + "/" + projectId + "/bind-score-sheet")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bindReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/" + projectId + "/activate")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        return projectId;
    }
}
