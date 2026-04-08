package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ScoreSheetApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String PROJECTS_BASE = "/review-projects";
    private static final String SS_BASE = "/score-sheets";
    private String hmToken = TestTokenFactory.hospitalManagerToken();
    private String saToken = TestTokenFactory.systemAdminToken();

    // --- Helper: create a project first (needed for HospitalIdResolver) ---

    private String createProject() throws Exception {
        String body = "{\"name\":\"Test Project\",\"medicalRecordTypes\":[\"OUTPATIENT\"]}";
        MvcResult result = mockMvc.perform(post(PROJECTS_BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).at("/data/projectId").asText();
    }

    private String createScoreSheet(String projectId) throws Exception {
        String body = "{\"projectId\":\"" + projectId + "\"}";
        MvcResult result = mockMvc.perform(post(SS_BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).at("/data/scoreSheetId").asText();
    }

    private void addSectionToSheet(String scoreSheetId) throws Exception {
        Map<String, Object> addItem = new LinkedHashMap<>();
        addItem.put("targetSectionId", "sec1");
        addItem.put("itemId", "i1");
        addItem.put("code", "C01");
        addItem.put("title", "Item 1");
        addItem.put("scoreType", "FIXED");
        addItem.put("maxScore", 10);
        addItem.put("weight", 1);
        addItem.put("requiredRule", "ALWAYS_REQUIRED");

        Map<String, Object> addSection = new LinkedHashMap<>();
        addSection.put("sectionId", "sec1");
        addSection.put("title", "General");
        addSection.put("items", List.of(addItem));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("addSections", List.of(addSection));

        mockMvc.perform(put(SS_BASE + "/" + scoreSheetId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    // --- CREATE ---

    @Test
    void createDraft_shouldReturn201() throws Exception {
        String projectId = createProject();
        String body = "{\"projectId\":\"" + projectId + "\"}";

        mockMvc.perform(post(SS_BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scoreSheetId").isNotEmpty());
    }

    @Test
    void createDraft_sa_shouldReturn403() throws Exception {
        String projectId = createProject();
        String body = "{\"projectId\":\"" + projectId + "\"}";

        mockMvc.perform(post(SS_BASE)
                        .header("api-key", saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // --- PUBLISH ---

    @Test
    void publish_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);
        addSectionToSheet(ssId);

        mockMvc.perform(post(SS_BASE + "/" + ssId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void publish_nonexistent_shouldReturn404() throws Exception {
        mockMvc.perform(post(SS_BASE + "/nonexistent/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }

    // --- UPDATE DRAFT ---

    @Test
    void updateDraft_addSection_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        Map<String, Object> addItem = new LinkedHashMap<>();
        addItem.put("targetSectionId", "sec1");
        addItem.put("itemId", "i1");
        addItem.put("code", "C01");
        addItem.put("title", "Item 1");
        addItem.put("scoreType", "FIXED");
        addItem.put("maxScore", 10);
        addItem.put("weight", 1);
        addItem.put("requiredRule", "ALWAYS_REQUIRED");

        Map<String, Object> addSection = new LinkedHashMap<>();
        addSection.put("sectionId", "sec1");
        addSection.put("title", "General");
        addSection.put("items", List.of(addItem));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("addSections", List.of(addSection));

        mockMvc.perform(put(SS_BASE + "/" + ssId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void updateDraft_onPublished_shouldReturn409() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);
        addSectionToSheet(ssId);
        mockMvc.perform(post(SS_BASE + "/" + ssId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        Map<String, Object> body = Map.of("removeSectionIds", List.of("sec1"));

        mockMvc.perform(put(SS_BASE + "/" + ssId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    // --- LIST ---

    @Test
    void list_hm_shouldReturn200() throws Exception {
        String projectId = createProject();
        createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE)
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void list_sa_shouldReturn200() throws Exception {
        String projectId = createProject();
        createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE)
                        .header("api-key", saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // --- DETAIL ---

    @Test
    void detail_hm_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE + "/" + ssId)
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreSheetId").value(ssId));
    }

    @Test
    void detail_sa_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE + "/" + ssId)
                        .header("api-key", saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreSheetId").value(ssId));
    }

    // --- VERSION ---

    @Test
    void version_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE + "/" + ssId + "/version")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.published").value(false));
    }

    // --- SECTIONS ---

    @Test
    void sections_hm_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);
        addSectionToSheet(ssId);

        mockMvc.perform(get(SS_BASE + "/" + ssId + "/sections")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sectionId").value("sec1"))
                .andExpect(jsonPath("$.data[0].items", hasSize(1)));
    }

    @Test
    void sections_sa_shouldReturn403() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE + "/" + ssId + "/sections")
                        .header("api-key", saToken))
                .andExpect(status().isForbidden());
    }
}
