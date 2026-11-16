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

    @Test
    void listScoreSheets_systemAdminHospitalIdNarrows_managerHospitalIdIgnored() throws Exception {
        // D-047 第 1、2 點
        createScoreSheet(createProject());
        String admin = TestTokenFactory.systemAdminToken();

        mockMvc.perform(get(SS_BASE).header("api-key", admin).param("hospitalId", "H001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)));
        mockMvc.perform(get(SS_BASE).header("api-key", admin).param("hospitalId", "H002"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get(SS_BASE).header("api-key", hmToken).param("hospitalId", "H002"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)));
    }

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
    void sections_sa_shouldReturn200_D050() throws Exception {
        // D-050 第 7 點：SYSTEM_ADMIN 可讀取評分表，不受院區限制（D-047）
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE + "/" + ssId + "/sections")
                        .header("api-key", saToken))
                .andExpect(status().isOk());
    }

    // --- ENUM VALIDATION ---

    @Test
    void updateDraft_nullListElements_shouldReturn400ValidationError() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        for (String phase : List.of("addSections", "addItems", "updateSections", "updateItems", "removeSectionIds", "removeItemIds")) {
            mockMvc.perform(put(SS_BASE + "/" + ssId)
                            .header("api-key", hmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"" + phase + "\":[null]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        mockMvc.perform(put(SS_BASE + "/" + ssId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addSections\":[{\"sectionId\":\"S1\",\"title\":\"T\",\"items\":[null]}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateDraft_invalidScoreType_shouldReturn400() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        Map<String, Object> addItem = new LinkedHashMap<>();
        addItem.put("targetSectionId", "sec1");
        addItem.put("itemId", "i1");
        addItem.put("code", "C01");
        addItem.put("title", "Item 1");
        addItem.put("scoreType", "INVALID_TYPE");
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message", containsString("scoreType")))
                .andExpect(jsonPath("$.error.message", containsString("INVALID_TYPE")));
    }

    @Test
    void updateDraft_invalidRequiredRule_shouldReturn400() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        Map<String, Object> updateItem = new LinkedHashMap<>();
        updateItem.put("itemId", "i1");
        updateItem.put("code", "C01");
        updateItem.put("title", "Item 1");
        updateItem.put("scoreType", "FIXED");
        updateItem.put("maxScore", 10);
        updateItem.put("weight", 1);
        updateItem.put("requiredRule", "BAD_RULE");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updateItems", List.of(updateItem));

        mockMvc.perform(put(SS_BASE + "/" + ssId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message", containsString("requiredRule")))
                .andExpect(jsonPath("$.error.message", containsString("BAD_RULE")));
    }

    @Test
    void updateDraft_nullScoreType_shouldReturn400() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        Map<String, Object> addItem = new LinkedHashMap<>();
        addItem.put("targetSectionId", "sec1");
        addItem.put("itemId", "i1");
        addItem.put("code", "C01");
        addItem.put("title", "Item 1");
        addItem.put("scoreType", null);
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
                .andExpect(status().isBadRequest())
                // SS-UPD-04：未提供列舉是欄位驗證錯誤，由 domain 依檢查順序回報（不是 controller 的 BAD_REQUEST）
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message", containsString("scoreType")));
    }

    @Test
    void updateDraft_emptyRequiredRule_shouldReturn400() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        Map<String, Object> updateItem = new LinkedHashMap<>();
        updateItem.put("itemId", "i1");
        updateItem.put("code", "C01");
        updateItem.put("title", "Item 1");
        updateItem.put("scoreType", "FIXED");
        updateItem.put("maxScore", 10);
        updateItem.put("weight", 1);
        updateItem.put("requiredRule", "");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updateItems", List.of(updateItem));

        mockMvc.perform(put(SS_BASE + "/" + ssId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message", containsString("requiredRule")));
    }
}
