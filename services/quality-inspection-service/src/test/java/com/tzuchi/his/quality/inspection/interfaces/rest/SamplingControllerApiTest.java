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
                new RuleSnapshot("project=" + projectId + ",version=1", "policy"));
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
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    // --- Helpers ---

    private String createAndActivateProject(String name) throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setHospitalId("H001");
        req.setName(name);
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

        // Activate directly via repository (no activate endpoint in B1/B2)
        com.tzuchi.his.quality.inspection.domain.project.ReviewProject project =
                projectRepository.findById(projectId)
                        .orElseThrow(() -> new AssertionError("Project not found: " + projectId));
        project.activate();
        projectRepository.save(project);

        return projectId;
    }
}
