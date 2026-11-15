package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;
import com.tzuchi.his.quality.inspection.domain.sampling.RuleSnapshot;
import com.tzuchi.his.quality.inspection.domain.sampling.SamplingBatch;
import com.tzuchi.his.quality.inspection.domain.sampling.SamplingBatchRepository;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateReviewProjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SamplingControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SamplingBatchRepository batchRepository;

    @Autowired
    private ReviewProjectRepository projectRepository;

    @Test
    void previewSampling_activeProject_shouldReturn200WithCounts() throws Exception {
        String projectId = createAndActivateProject("Preview Test");

        mockMvc.perform(post("/review-projects/" + projectId + "/sampling/preview")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.populationCount").exists())
                .andExpect(jsonPath("$.data.eligibleCount").exists())
                .andExpect(jsonPath("$.data.excludedCount").exists())
                .andExpect(jsonPath("$.data.sourceCaseKeys").doesNotExist());
    }

    @Test
    void getExecuteContext_activeProject_shouldReturnBootstrapContext() throws Exception {
        String projectId = createAndActivateProject("Execute Context Test");

        mockMvc.perform(get("/review-projects/" + projectId + "/sampling/execute-context")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.projectName").value("Execute Context Test"))
                .andExpect(jsonPath("$.data.hospitalId").value("H001"))
                .andExpect(jsonPath("$.data.projectStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andExpect(jsonPath("$.data.scoreSheetId").value("ss-test"))
                .andExpect(jsonPath("$.data.previewReady").value(false))
                .andExpect(jsonPath("$.data.executing").value(false))
                .andExpect(jsonPath("$.data.executeFailed").value(false))
                .andExpect(jsonPath("$.data.executedBatchId").value(nullValue()))
                .andExpect(jsonPath("$.data.previewStats").value(nullValue()))
                .andExpect(jsonPath("$.data.postExecuteSummary").value(nullValue()));
    }

    @Test
    void getExecuteContext_failedBatch_shouldReturnFailureSummary() throws Exception {
        String projectId = createAndActivateProject("Execute Context Failed Test");

        SamplingBatch failedBatch = SamplingBatch.start(
                "failed-context-batch", projectId, ExecutionMode.MANUAL,
                new RuleSnapshot("PV-" + projectId + "-v1", 1, "SS-001-v1", "MANUAL", null));
        failedBatch.fail("simulated failure");
        batchRepository.save(failedBatch);

        mockMvc.perform(get("/review-projects/" + projectId + "/sampling/execute-context")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executeFailed").value(true))
                .andExpect(jsonPath("$.data.postExecuteSummary.tone").value("failed"))
                .andExpect(jsonPath("$.data.postExecuteSummary.batchId").value("failed-context-batch"));
    }

    @Test
    void getExecuteContext_completedBatch_shouldReturnSuccessSummary() throws Exception {
        String projectId = createAndActivateProject("Execute Context Completed Test");

        MvcResult execResult = mockMvc.perform(
                        post("/review-projects/" + projectId + "/sampling/execute")
                                .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated())
                .andReturn();

        String batchId = objectMapper.readTree(
                execResult.getResponse().getContentAsString())
                .get("data").get("batchId").asText();

        mockMvc.perform(get("/review-projects/" + projectId + "/sampling/execute-context")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executedBatchId").value(batchId))
                .andExpect(jsonPath("$.data.postExecuteSummary.tone").value("success"))
                .andExpect(jsonPath("$.data.postExecuteSummary.batchId").value(batchId));
    }

    @Test
    void executeSampling_activeProject_shouldReturn201WithBatchId() throws Exception {
        String projectId = createAndActivateProject("Execute Test");

        mockMvc.perform(post("/review-projects/" + projectId + "/sampling/execute")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").isNotEmpty());
    }

    @Test
    void retrySampling_failedBatch_shouldReturn201WithNewBatchId() throws Exception {
        String projectId = createAndActivateProject("Retry Test");

        SamplingBatch failedBatch = SamplingBatch.start(
                "test-failed-batch", projectId, ExecutionMode.MANUAL,
                new RuleSnapshot("PV-" + projectId + "-v1", 1, "SS-001-v1", "MANUAL", null));
        failedBatch.fail("simulated failure");
        batchRepository.save(failedBatch);

        MvcResult result = mockMvc.perform(
                        post("/sampling-batches/test-failed-batch/retry")
                                .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").isNotEmpty())
                .andReturn();

        String newBatchId = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .get("data").get("batchId").asText();
        assert !newBatchId.equals("test-failed-batch") : "Retry must produce a new batchId";
    }

    @Test
    void retrySampling_completedBatch_shouldReturn422() throws Exception {
        String projectId = createAndActivateProject("Retry-Completed Test");

        MvcResult execResult = mockMvc.perform(
                        post("/review-projects/" + projectId + "/sampling/execute")
                                .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated())
                .andReturn();

        String batchId = objectMapper.readTree(
                execResult.getResponse().getContentAsString())
                .get("data").get("batchId").asText();

        mockMvc.perform(post("/sampling-batches/" + batchId + "/retry")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BATCH_NOT_RETRYABLE"));
    }

    @Test
    void executeSampling_nonManagerRole_shouldReturn403() throws Exception {
        mockMvc.perform(post("/review-projects/any-id/sampling/execute")
                        .header("api-key", TestTokenFactory.unknownRoleToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ROLE_DENIED"));
    }

    @Test
    void listBatches_shouldReturn200WithPage() throws Exception {
        String projectId = createAndActivateProject("List Test");

        mockMvc.perform(post("/review-projects/" + projectId + "/sampling/execute")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/sampling-batches")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .param("projectId", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].batchId").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].projectName").value("List Test"))
                .andExpect(jsonPath("$.data.content[0].projectVersionNumber").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listBatches_systemAdminHospitalIdNarrows_managerHospitalIdIgnored() throws Exception {
        // D-047 第 1、2 點
        String projectId = createAndActivateProject("Scope Test");
        mockMvc.perform(post("/review-projects/" + projectId + "/sampling/execute")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated());
        String admin = TestTokenFactory.systemAdminToken();

        mockMvc.perform(get("/sampling-batches").header("api-key", admin).param("hospitalId", "H001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/sampling-batches").header("api-key", admin).param("hospitalId", "H002"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/sampling-batches")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()).param("hospitalId", "H002"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listBatches_invalidStatus_shouldReturn400() throws Exception {
        mockMvc.perform(get("/sampling-batches")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void getBatchDetail_shouldReturn200WithDetail() throws Exception {
        String projectId = createAndActivateProject("Detail Test");

        MvcResult execResult = mockMvc.perform(
                        post("/review-projects/" + projectId + "/sampling/execute")
                                .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated())
                .andReturn();

        String batchId = objectMapper.readTree(
                execResult.getResponse().getContentAsString())
                .get("data").get("batchId").asText();

        mockMvc.perform(get("/sampling-batches/" + batchId)
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").value(batchId))
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.ruleSnapshot.projectVersionId").isNotEmpty())
                .andExpect(jsonPath("$.data.selectedCases").isArray());
    }

    @Test
    void getBatchDetail_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/sampling-batches/nonexistent")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SAMPLING_BATCH_NOT_FOUND"));
    }

    @Test
    void exportBatchResult_shouldReturn200() throws Exception {
        String projectId = createAndActivateProject("Export Test");

        MvcResult execResult = mockMvc.perform(
                        post("/review-projects/" + projectId + "/sampling/execute")
                                .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated())
                .andReturn();

        String batchId = objectMapper.readTree(
                execResult.getResponse().getContentAsString())
                .get("data").get("batchId").asText();

        mockMvc.perform(get("/sampling-batches/" + batchId + "/export")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.detail.batchId").value(batchId));
    }

    @Test
    void listBatches_systemAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(get("/sampling-batches")
                        .header("api-key", TestTokenFactory.systemAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listBatches_unknownRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/sampling-batches")
                        .header("api-key", TestTokenFactory.unknownRoleToken()))
                .andExpect(status().isForbidden());
    }

    // --- Helpers ---

    private String createAndActivateProject(String name) throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setName(name);
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));
        req.setQualityThreshold(new java.math.BigDecimal("5"));

        MvcResult result = mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String projectId = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .get("data").get("projectId").asText();

        // 發布前必須已綁定評分表（D-041、RP-PUB-02）
        com.tzuchi.his.quality.inspection.domain.project.ReviewProject unbound =
                projectRepository.findById(projectId)
                        .orElseThrow(() -> new AssertionError("Project not found: " + projectId));
        unbound.bindScoreSheet("ss-test");
        projectRepository.save(unbound);

        mockMvc.perform(post("/review-projects/" + projectId + "/publish")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk());

        // Activate directly via repository (no activate endpoint in B1/B2)
        com.tzuchi.his.quality.inspection.domain.project.ReviewProject project =
                projectRepository.findById(projectId)
                        .orElseThrow(() -> new AssertionError("Project not found: " + projectId));
        project.activate();
        projectRepository.save(project);

        return projectId;
    }
}
