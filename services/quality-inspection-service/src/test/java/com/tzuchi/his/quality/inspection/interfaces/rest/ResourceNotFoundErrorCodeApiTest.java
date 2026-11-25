package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 找不到資源時回 404，錯誤代碼是 api-spec／D-054 的代碼（CASE_NOT_FOUND、SUBMISSION_NOT_FOUND），
 * 不是實體名稱（"ReviewCase"、"ReviewSubmission"）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResourceNotFoundErrorCodeApiTest {

    private static final String ASSIGN_BODY = "{\"rereviewerId\":\"rr2\",\"dueAt\":\"2026-12-31T23:59:00+08:00\"}";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ReviewCaseJpaRepository cases;

    @BeforeEach
    void seed() {
        cases.deleteAll();
        ReviewCaseEntity rc = new ReviewCaseEntity();
        rc.setCaseId("c1"); rc.setBatchId("b"); rc.setProjectId("p"); rc.setHospitalId("H001");
        rc.setStatus(CaseStatus.REREVIEW_IN_PROGRESS); rc.setReviewerId("initial");
        rc.setRereviewerId("rr"); rc.setRereviewerAssignedAt(Instant.parse("2026-01-01T00:00:00Z"));
        cases.save(rc);
    }

    @AfterEach
    void cleanUp() {
        cases.deleteAll();
    }

    static Stream<Arguments> missingCase() {
        return Stream.of(
                Arguments.of("GET", "/review-cases/missing", "reviewer", null),
                Arguments.of("GET", "/review-cases/missing/revision-history", "reviewer", null),
                Arguments.of("GET", "/review-cases/missing/assignment-context", "hospital_manager", null),
                Arguments.of("GET", "/review-cases/missing/assignment-candidates?role=REREVIEWER", "hospital_manager", null),
                Arguments.of("POST", "/review-cases/missing/close-without-rereview", "hospital_manager", null),
                Arguments.of("POST", "/review-cases/missing/send-to-rereview", "hospital_manager", null),
                Arguments.of("POST", "/review-cases/missing/assign-rereviewer", "hospital_manager", ASSIGN_BODY),
                Arguments.of("POST", "/review-cases/missing/reassign-rereviewer", "hospital_manager", ASSIGN_BODY),
                Arguments.of("POST", "/review-cases/missing/start-rereview", "rereviewer", null),
                Arguments.of("POST", "/review-cases/missing/rereview/draft", "rereviewer", "{\"submissionId\":\"s\",\"answers\":[]}"),
                Arguments.of("POST", "/review-cases/missing/rereview/submit", "rereviewer", "{\"submissionId\":\"s\"}"),
                Arguments.of("POST", "/review-cases/missing/rereview/submissions/s/close", "rereviewer", null),
                Arguments.of("POST", "/review-cases/missing/rereview/submissions/s/revise", "rereviewer", "{}"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("missingCase")
    void missingCase_returnsCaseNotFound(String method, String path, String role, String body) throws Exception {
        perform(method, path, role, body)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CASE_NOT_FOUND"));
    }

    static Stream<Arguments> missingSubmission() {
        return Stream.of(
                Arguments.of("GET", "/review-submissions/missing", "hospital_manager", null),
                Arguments.of("POST", "/review-cases/c1/rereview/draft", "rereviewer", "{\"submissionId\":\"missing\",\"answers\":[]}"),
                Arguments.of("POST", "/review-cases/c1/rereview/submit", "rereviewer", "{\"submissionId\":\"missing\"}"),
                Arguments.of("POST", "/review-cases/c1/rereview/submissions/missing/close", "rereviewer", null),
                Arguments.of("POST", "/review-cases/c1/rereview/submissions/missing/revise", "rereviewer", "{}"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("missingSubmission")
    void missingSubmission_returnsSubmissionNotFound(String method, String path, String role, String body) throws Exception {
        perform(method, path, role, body)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SUBMISSION_NOT_FOUND"));
    }

    private org.springframework.test.web.servlet.ResultActions perform(String method, String path, String role, String body)
            throws Exception {
        var builder = request(HttpMethod.valueOf(method), path).header("api-key", token(role.equals("rereviewer") ? "rr" : "u1", role));
        if (body != null) builder = builder.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(builder);
    }

    private String token(String user, String role) throws Exception {
        String tzUser = mapper.writeValueAsString(Map.of("user", Map.of("user_id", user, "user_name", user,
                "hospital_id", "H001", "user_role", List.of(Map.of("role_name", role, "enabled", true)))));
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString("{\"alg\":\"RS512\",\"typ\":\"JWT\"}".getBytes()) + "."
                + b64.encodeToString(mapper.writeValueAsBytes(Map.of("tz-user", tzUser))) + ".test-sig";
    }
}
