package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.*;
import com.tzuchi.his.quality.inspection.application.common.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.AssignRereviewerUseCase;
import com.tzuchi.his.quality.inspection.application.reviewcase.ReassignRereviewerUseCase;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.*;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.*;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.*;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.*;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.MockMvc;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@RecordApplicationEvents
class PermanentBlindReviewReadApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ReviewCaseJpaRepository cases;
    @Autowired private ReviewSubmissionJpaRepository submissions;
    @Autowired private AuditTrailJpaRepository audit;
    @Autowired private ApplicationEvents events;
    @Autowired private AssignRereviewerUseCase assign;
    @Autowired private ReassignRereviewerUseCase reassign;
    @Autowired private ReviewProjectJpaRepository projects;
    @Autowired private com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess.InitialReviewReaderJpaRepository readers;
    @Autowired private com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess.RereviewerAssignmentHistoryJpaRepository history;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final String[] paths = {"/review-submissions/i1", "/review-submissions/i2",
            "/review-cases/c1", "/review-cases/c1/revision-history"};

    @BeforeEach
    void seed() {
        readers.deleteAll(); history.deleteAll();
        audit.deleteAll(); submissions.deleteAll(); cases.deleteAll();
        ReviewProjectEntity project = new ReviewProjectEntity();
        project.setProjectId("p"); project.setName("Reviewed Project"); project.setHospitalId("H001");
        project.setStatus(ProjectStatus.ACTIVE); projects.save(project);
        ReviewCaseEntity rc = new ReviewCaseEntity();
        rc.setCaseId("c1"); rc.setBatchId("b"); rc.setProjectId("p"); rc.setHospitalId("H001");
        rc.setStatus(CaseStatus.PENDING_REREVIEW); rc.setReviewerId("initial");
        rc.setReviewerAssignedAt(Instant.parse("2026-01-01T00:00:00Z"));
        rc.setRereviewerId("rr"); rc.setRereviewerAssignedAt(rc.getReviewerAssignedAt());
        rc.setInitialReviewScore(new BigDecimal("92")); rc.setFinalScore(new BigDecimal("86"));
        rc.setFinalConclusion("SECRET_INITIAL_CONCLUSION");
        rc.setLatestInitialSubmissionId("i2"); rc.setLatestRereviewSubmissionId("r1");
        cases.save(rc);
        submissions.save(sub("i1", ReviewStage.INITIAL, "initial", SubmissionStatus.SUPERSEDED));
        submissions.save(sub("i2", ReviewStage.INITIAL, "initial", SubmissionStatus.SUBMITTED));
        AuditTrailEntity existingAudit = new AuditTrailEntity();
        existingAudit.setId("existing-initial-audit"); existingAudit.setEntityType("ReviewCase");
        existingAudit.setEntityId("c1"); existingAudit.setAction("INITIAL_SUBMITTED");
        existingAudit.setActorUserId("initial"); existingAudit.setActorUserName("Initial Doctor");
        existingAudit.setHospitalId("H001"); existingAudit.setOccurredAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        existingAudit.setDetail("SECRET_INITIAL_AUDIT_SCORE=92"); audit.save(existingAudit);
    }

    /** D-055 的讀取者紀錄與指派歷史會 commit；清掉，避免影響共用同一個資料庫的其他測試（例如指派被 422 擋下）。 */
    @AfterEach
    void cleanUpAccessRecords() {
        readers.deleteAll(); history.deleteAll();
    }

    private ReviewSubmissionEntity sub(String id, ReviewStage stage, String owner, SubmissionStatus state) {
        ReviewSubmissionEntity s = new ReviewSubmissionEntity();
        s.setSubmissionId(id); s.setCaseId("c1"); s.setStage(stage); s.setReviewerId(owner); s.setStatus(state);
        s.setRevisionNo(id.equals("i2") ? 2 : 1);
        s.setAnswerSet(List.of(AnswerItem.ofScored("Q", stage == ReviewStage.INITIAL ? "SECRET_INITIAL_ANSWER" : "own answer", BigDecimal.TEN)));
        s.setTotalScore(new BigDecimal(stage == ReviewStage.INITIAL ? "92" : "80"));
        s.setComment(stage == ReviewStage.INITIAL ? "SECRET_INITIAL_COMMENT" : "own comment");
        s.setAttachmentRefsJson(stage == ReviewStage.INITIAL ? "SECRET_INITIAL_ATTACHMENT" : "own attachment");
        s.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z")); s.setSubmittedAt(s.getCreatedAt());
        if (state == SubmissionStatus.SUPERSEDED) s.setSupersededBySubmissionId("i2");
        return s;
    }

    private String token(String user, String hospital, String... roles) throws Exception {
        List<Map<String, Object>> roleClaims = new ArrayList<>();
        for (String r : roles) roleClaims.add(Map.of("role_name", r, "enabled", true));
        String tzUser = mapper.writeValueAsString(Map.of("user", Map.of("user_id", user,
                "user_name", user, "hospital_id", hospital, "user_role", roleClaims)));
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString("{\"alg\":\"RS512\",\"typ\":\"JWT\"}".getBytes()) + "."
                + b64.encodeToString(mapper.writeValueAsBytes(Map.of("tz-user", tzUser))) + ".test-sig";
    }

    private JsonNode read(String path, String token) throws Exception {
        return mapper.readTree(mvc.perform(get(path).header("api-key", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    }

    private void absent(JsonNode n, String field) {
        assertTrue(n.path(field).isMissingNode() || n.path(field).isNull(), field + " leaked: " + n);
    }

    @ParameterizedTest
    @ValueSource(strings = {"before", "progress", "submitted", "closed", "historical"})
    void rereviewerCannotReadInitialContentAcrossAnyScopedPath(String phase) throws Exception {
        ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
        if (!phase.equals("before")) {
            rc.setStatus(phase.equals("closed") ? CaseStatus.CLOSED : CaseStatus.REREVIEW_IN_PROGRESS);
            submissions.save(sub("r1", ReviewStage.REREVIEW, "rr",
                    phase.equals("progress") ? SubmissionStatus.DRAFT : SubmissionStatus.SUBMITTED));
        }
        if (phase.equals("historical")) rc.setRereviewerId("new-rereviewer");
        cases.save(rc);
        Map<String, String> before = snapshot(); long audits = audit.count();
        long domainEvents = events.stream(DomainEvent.class).count();
        String auth = token("rr", "H001", "rereviewer");
        for (String path : paths) {
            JsonNode data = read(path, auth);
            assertFalse(data.toString().contains("SECRET_INITIAL"), "Secret leaked via " + path);
            for (String field : List.of("initialReviewScore", "finalScore", "initialTotalScore", "finalConclusion")) {
                for (JsonNode value : data.findValues(field)) assertTrue(value.isNull(), field + " leaked via " + path);
            }
            if (path.startsWith("/review-submissions")) {
                for (String field : List.of("answers", "comment", "totalScore", "attachmentRefsJson")) absent(data, field);
                assertTrue(data.path("maskedFields").toString().contains("totalScore"));
            }
            for (String list : List.of("submissions", "entries")) {
                for (JsonNode entry : data.path(list)) {
                    if (entry.path("stage").asText().equals("INITIAL")) {
                        absent(entry, "totalScore"); absent(entry, "comment");
                    }
                }
            }
        }
        if (!phase.equals("before")) {
            JsonNode own = read("/review-submissions/r1", auth);
            assertEquals("own comment", own.path("comment").asText());
            assertEquals(80, own.path("totalScore").asInt());
        }
        assertEquals(before, snapshot()); assertEquals(audits, audit.count());
        assertEquals(domainEvents, events.stream(DomainEvent.class).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"crossHospital", "unrelated", "managerCrossHospital", "auditorCrossHospital"})
    void unauthorizedCasesAreDeniedOnAllScopedPaths(String scenario) throws Exception {
        String auth = token(scenario.equals("unrelated") ? "stranger" : "rr",
                scenario.equals("unrelated") ? "H001" : "H002",
                scenario.equals("managerCrossHospital") ? "hospital_manager" : scenario.equals("auditorCrossHospital") ? "auditor" : "rereviewer");
        for (String path : paths) {
            org.springframework.test.web.servlet.MvcResult response = mvc.perform(get(path).header("api-key", auth))
                    .andExpect(status().isForbidden()).andReturn();
            assertEquals(scenario.equals("unrelated") ? "ACCESS_DENIED" : "HOSPITAL_SCOPE_MISMATCH",
                    mapper.readTree(response.getResponse().getContentAsString()).path("error").path("code").asText());
        }
    }

    /** D-055 第 3 點：歷史完整且不在歷史中 → Proven non-historical，可完整讀取（取代 D-035 時期的全面拒絕）。 */
    @ParameterizedTest
    @ValueSource(strings = {"hospital_manager", "auditor", "reviewer"})
    void singleRoleFullReadersWithCompleteHistoryCanRead_D055(String role) throws Exception {
        String user = role.equals("reviewer") ? "initial" : "full-reader";
        String auth = token(user, "H001", role);
        for (String path : paths) mvc.perform(get(path).header("api-key", auth)).andExpect(status().isOk());
        assertTrue(read("/review-submissions/i2", auth).toString().contains("SECRET_INITIAL_ANSWER"));
        assertEquals(List.of(user), readerIds());
    }

    /** D-055 第 3 點：歷史不完整仍為 UNKNOWN，拒絕且不留下任何寫入（含讀取者紀錄）。 */
    @ParameterizedTest
    @ValueSource(strings = {"hospital_manager", "auditor", "reviewer"})
    void singleRoleFullReadersWithIncompleteHistoryAreDenied_D055(String role) throws Exception {
        markHistoryIncomplete();
        Map<String, String> before = snapshot();
        String auth = token(role.equals("reviewer") ? "initial" : "full-reader", "H001", role);
        for (String path : paths) mvc.perform(get(path).header("api-key", auth)).andExpect(status().isForbidden());
        assertEquals(before, snapshot());
        assertTrue(readerIds().isEmpty());
    }

    private void markHistoryIncomplete() {
        // 只在測試用的內嵌資料庫模擬 V5 之前就存在的案件；正式程式沒有任何寫入這個欄位的路徑。
        jdbc.update("update review_case set rereviewer_history_complete = 0 where case_id = 'c1'");
    }

    private List<String> readerIds() {
        List<String> ids = new ArrayList<>();
        readers.findAll().forEach(r -> ids.add(r.getReaderId()));
        Collections.sort(ids);
        return ids;
    }

    @ParameterizedTest
    @ValueSource(strings = {"hospital_manager", "reviewer", "auditor", "system_admin"})
    void unresolvedMixedRoleRequestsCannotBypassBlindReview(String otherRole) throws Exception {
        String auth = token("rr", "H001", "rereviewer", otherRole);
        for (String path : paths) mvc.perform(get(path).header("api-key", auth)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"hospital_manager", "auditor"})
    void sameCaseRereviewerCannotSwitchToFullReaderRole(String role) throws Exception {
        String auth = token("rr", "H001", role);
        for (String path : paths) mvc.perform(get(path).header("api-key", auth)).andExpect(status().isForbidden());
    }

    /** D-033：生命週期的稽核 detail 帶有初審分數與 finalScore，複審醫師與 metadata-only 的系統管理者不得取得。 */
    @ParameterizedTest
    @ValueSource(strings = {"rereviewer", "system_admin", "auditor+rereviewer", "hospital_manager+rereviewer"})
    void lifecycleWithholdsAuditDetailFromRereviewersAndSystemAdmin_D033(String roles) throws Exception {
        String user = roles.equals("system_admin") ? "admin" : "rr";
        JsonNode data = read("/review-cases/c1/lifecycle", token(user, "H001", roles.split("\\+")));
        assertMetadataOnlyAuditEntries(data, roles);
    }

    /** D-033：同時持有複審醫師角色的人不可改從稽核軌跡列表讀到同一份 detail。 */
    @ParameterizedTest
    @ValueSource(strings = {"auditor+rereviewer", "hospital_manager+rereviewer"})
    void auditTrailListWithholdsDetailFromActorsHoldingRereviewer_D033(String roles) throws Exception {
        JsonNode data = mapper.readTree(mvc.perform(get("/audit-trails").param("aggregateId", "c1")
                        .header("api-key", token("rr", "H001", roles.split("\\+"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        assertMetadataOnlyAuditEntries(data.path("content"), roles);
    }

    /** 回應必須含 seed 的那筆紀錄（排除 403、500 或空清單也算成功），且每筆都不帶 detail。 */
    private void assertMetadataOnlyAuditEntries(JsonNode entries, String roles) {
        assertTrue(entries.isArray() && entries.size() > 0, "No audit entries returned to " + roles + ": " + entries);
        List<String> ids = new ArrayList<>();
        for (JsonNode entry : entries) {
            ids.add(entry.path("id").asText());
            absent(entry, "detail");
        }
        assertTrue(ids.contains("existing-initial-audit"), "Seeded audit entry missing for " + roles + ": " + ids);
        assertFalse(entries.toString().contains("SECRET_INITIAL"), "Audit detail leaked to " + roles);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hospital_manager", "auditor"})
    void lifecycleStillShowsAuditDetailToFullReaders(String role) throws Exception {
        JsonNode data = read("/review-cases/c1/lifecycle", token("m", "H001", role));
        assertEquals("SECRET_INITIAL_AUDIT_SCORE=92", data.get(0).path("detail").asText());
    }

    @Test
    void systemAdminGetsMetadataWithoutScoresOrClinicalContent() throws Exception {
        String auth = token("admin", "H001", "system_admin");
        for (String path : paths) {
            JsonNode data = read(path, auth);
            assertFalse(data.toString().contains("SECRET_INITIAL"));
            for (String field : List.of("answers", "comment", "totalScore", "initialReviewScore", "finalScore", "initialTotalScore", "finalConclusion", "attachmentRefsJson")) {
                for (JsonNode value : data.findValues(field)) assertTrue(value.isNull(), field + " leaked");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"manager-active", "auditor-active", "manager-closed", "auditor-closed"})
    void historicalAssignmentBeforeSubmissionPreventsAlternateRoleBypass(String scenario) throws Exception {
        ActorContext manager = new ActorContext("manager", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        ReviewCaseEntity unassigned = cases.findById("c1").orElseThrow();
        unassigned.setRereviewerId(null); unassigned.setRereviewerAssignedAt(null); cases.save(unassigned);
        assign.execute(manager, "c1", "A", Instant.parse("2026-12-31T23:59:00Z"));
        ReassignRereviewerCommand next = new ReassignRereviewerCommand();
        next.setCaseId("c1"); next.setRereviewerId("B");
        reassign.execute(manager, next);
        assertEquals("B", cases.findById("c1").orElseThrow().getRereviewerId());
        assertTrue(submissions.findByCaseIdAndStage("c1", ReviewStage.REREVIEW).isEmpty());
        Set<String> recordedAssignees = new HashSet<>();
        for (AuditTrailEntity entry : audit.findAll()) {
            if (entry.getAction().equals("REREVIEWER_ASSIGNED")) {
                assertEquals("c1", entry.getEntityId()); assertEquals("H001", entry.getHospitalId());
                recordedAssignees.add(mapper.readTree(entry.getDetail()).path("rereviewerId").asText());
            }
        }
        assertEquals(Set.of("A", "B"), recordedAssignees);
        if (scenario.endsWith("closed")) {
            ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
            rc.setStatus(CaseStatus.CLOSED); cases.save(rc);
        }
        Map<String, String> before = snapshot();
        String oldDoctor = token("A", "H001", scenario.startsWith("manager") ? "hospital_manager" : "auditor");
        for (String path : paths) mvc.perform(get(path).header("api-key", oldDoctor)).andExpect(status().isForbidden());
        String currentDoctor = token("B", "H001", "rereviewer");
        absent(read(paths[0], currentDoctor), "comment");
        assertTrue(read(paths[2], currentDoctor).path("initialReviewScore").isNull());
        for (JsonNode entry : read(paths[3], currentDoctor).path("submissions")) assertTrue(entry.path("totalScore").isNull());
        assertEquals(before, snapshot());
        assertTrue(readerIds().isEmpty());
        // D-055：A 與 B 都在複審指派歷史中；指派操作者 manager 與 other-auditor 不在其中，歷史完整 → 可讀並記錄讀取者。
        mvc.perform(get(paths[2]).header("api-key", token("manager", "H001", "hospital_manager")))
                .andExpect(status().isOk());
        mvc.perform(get(paths[0]).header("api-key", token("other-auditor", "H001", "auditor")))
                .andExpect(status().isOk());
        assertEquals(List.of("manager", "other-auditor"), readerIds());
    }

    @Test
    void systemAdminCaseDetailUsesExactFormalAllowlist() throws Exception {
        Map<String, String> before = snapshot();
        JsonNode data = read("/review-cases/c1", token("admin", "H001", "system_admin"));
        Set<String> fields = new HashSet<>(); data.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("caseId", "caseStatus", "projectName", "hospitalId", "reviewerId", "rereviewerId",
                "updatedAt", "latestRevisionSummary"), fields);
        assertFalse(data.has("lifecycleLatestStage"));
        assertEquals("c1", data.path("caseId").asText());
        assertEquals("PENDING_REREVIEW", data.path("caseStatus").asText());
        assertEquals("Reviewed Project", data.path("projectName").asText());
        assertEquals("H001", data.path("hospitalId").asText());
        assertEquals("initial", data.path("reviewerId").asText());
        assertEquals("rr", data.path("rereviewerId").asText());
        for (String unknown : List.of("updatedAt", "latestRevisionSummary")) {
            assertTrue(data.path(unknown).isNull(), "Do not invent values for unavailable/undefined metadata");
        }
        assertFalse(data.has("initialReviewContext")); assertFalse(data.has("rereviewContext"));
        assertEquals(before, snapshot());
    }

    static Stream<Arguments> completenessCases() {
        List<Arguments> cases = new ArrayList<>();
        for (String evidence : List.of("valid", "missingA", "missingAll", "missingB", "malformedA")) {
            String knowledge = evidence.equals("valid") || evidence.equals("missingB")
                    ? "PROVEN_HISTORICAL" : "UNKNOWN";
            for (String role : List.of("hospital_manager", "auditor")) {
                for (boolean closed : List.of(false, true)) {
                    for (String path : List.of("/review-submissions/i2", "/review-cases/c1", "/review-cases/c1/revision-history")) {
                        cases.add(Arguments.of(evidence, knowledge, role, closed, path));
                    }
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("completenessCases")
    void incompleteHistoryMustNotGrantInitialContent(String evidence, String knowledge,
                                                     String role, boolean closed, String path) throws Exception {
        assignmentEvidenceFixture(evidence, closed);
        Map<String, String> before = snapshot();
        long beforeEvents = events.stream(DomainEvent.class).count();
        org.springframework.test.web.servlet.MvcResult result = mvc.perform(get(path)
                .header("api-key", token("A", "H001", role))).andReturn();
        assertEquals(before, snapshot());
        assertEquals(beforeEvents, events.stream(DomainEvent.class).count());
        assertEquals(403, result.getResponse().getStatus(), knowledge + "/" + evidence);
        JsonNode rejection = mapper.readTree(result.getResponse().getContentAsString());
        assertEquals("ACCESS_DENIED", rejection.path("error").path("code").asText());
        assertTrue(rejection.path("data").isMissingNode() || rejection.path("data").isNull());
    }

    static Stream<Arguments> currentReaderControls() {
        List<Arguments> cases = new ArrayList<>();
        for (boolean closed : List.of(false, true)) {
            for (String path : List.of("/review-submissions/i2", "/review-cases/c1", "/review-cases/c1/revision-history")) {
                cases.add(Arguments.of(closed, path));
            }
        }
        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("currentReaderControls")
    void currentBRemainsBlindEvenWithNoAssignmentAudit(boolean closed, String path) throws Exception {
        assignmentEvidenceFixture("missingAll", closed);
        Map<String, String> before = snapshot();
        org.springframework.test.web.servlet.MvcResult result = mvc.perform(get(path)
                .header("api-key", token("B", "H001", "rereviewer"))).andExpect(status().isOk()).andReturn();
        assertProtectedContentOrDenied(result, "B has proven current identity");
        assertEquals(before, snapshot());
    }

    static Stream<Arguments> legitimateReaderControls() {
        List<Arguments> cases = new ArrayList<>();
        for (String role : List.of("hospital_manager", "auditor")) {
            for (boolean noRereview : List.of(false, true)) {
                for (String path : List.of("/review-submissions/i2", "/review-cases/c1", "/review-cases/c1/revision-history")) {
                    cases.add(Arguments.of(role, noRereview, path));
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("legitimateReaderControls")
    void existingFullReaderWithCompleteHistoryCanRead_D055(String role, boolean noRereview, String path) throws Exception {
        fullReaderFixture(noRereview);
        mvc.perform(get(path).header("api-key", token("unrelated-full-reader", "H001", role)))
                .andExpect(status().isOk());
        assertEquals(List.of("unrelated-full-reader"), readerIds());
    }

    @ParameterizedTest
    @MethodSource("legitimateReaderControls")
    void existingFullReaderWithIncompleteHistoryIsDenied_D055(String role, boolean noRereview, String path) throws Exception {
        fullReaderFixture(noRereview);
        markHistoryIncomplete();
        Map<String, String> before = snapshot();
        mvc.perform(get(path).header("api-key", token("unrelated-full-reader", "H001", role)))
                .andExpect(status().isForbidden());
        assertEquals(before, snapshot());
        assertTrue(readerIds().isEmpty());
    }

    private void fullReaderFixture(boolean noRereview) throws Exception {
        if (noRereview) {
            ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
            rc.setStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);
            rc.setRereviewerId(null); rc.setRereviewerAssignedAt(null); rc.setLatestRereviewSubmissionId(null);
            rc.setRereviewRequired(false); rc.setFinalScore(rc.getInitialReviewScore()); cases.save(rc);
        } else {
            assignmentEvidenceFixture("missingAll", false);
        }
    }

    private void assignmentEvidenceFixture(String evidence, boolean closed) throws Exception {
        ReviewCaseEntity unassigned = cases.findById("c1").orElseThrow();
        unassigned.setRereviewerId(null); unassigned.setRereviewerAssignedAt(null); cases.save(unassigned);
        ActorContext manager = new ActorContext("manager", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        assign.execute(manager, "c1", "A", Instant.parse("2026-12-31T23:59:00Z"));
        ReassignRereviewerCommand next = new ReassignRereviewerCommand(); next.setCaseId("c1"); next.setRereviewerId("B");
        reassign.execute(manager, next);
        assertTrue(submissions.findByCaseIdAndStage("c1", ReviewStage.REREVIEW).isEmpty());
        // Simulate missing/damaged legacy evidence only inside this isolated embedded test database.
        // This fixture manipulation is not a repair or modification of production Audit data.
        for (AuditTrailEntity entry : audit.findAll()) {
            if (!entry.getAction().equals("REREVIEWER_ASSIGNED")) continue;
            String subject = mapper.readTree(entry.getDetail()).path("rereviewerId").asText();
            if (evidence.equals("missingAll") || evidence.equals("missingA") && subject.equals("A")
                    || evidence.equals("missingB") && subject.equals("B")) audit.deleteById(entry.getId());
            else if (evidence.equals("malformedA") && subject.equals("A")) {
                entry.setDetail("broken assignment JSON"); audit.save(entry);
            }
        }
        if (closed) {
            ReviewCaseEntity rc = cases.findById("c1").orElseThrow(); rc.setStatus(CaseStatus.CLOSED); cases.save(rc);
        }
        assertEquals("B", cases.findById("c1").orElseThrow().getRereviewerId());
    }

    private void assertProtectedContentOrDenied(org.springframework.test.web.servlet.MvcResult response, String knowledge) throws Exception {
        int code = response.getResponse().getStatus();
        if (code == 403) return;
        assertEquals(200, code, "Unexpected error is not a valid protection result");
        JsonNode data = mapper.readTree(response.getResponse().getContentAsString()).path("data");
        assertFalse(data.toString().contains("SECRET_INITIAL"), knowledge + " must not grant initial content");
        for (String field : List.of("initialReviewScore", "initialTotalScore", "finalScore", "finalConclusion")) {
            for (JsonNode value : data.findValues(field)) assertTrue(value.isNull(), knowledge + ": " + field);
        }
        if (data.has("stage") && data.path("stage").asText().equals("INITIAL")) {
            absent(data, "answers"); absent(data, "comment"); absent(data, "totalScore");
        }
        for (JsonNode item : data.path("submissions")) {
            if (item.path("stage").asText().equals("INITIAL")) { absent(item, "comment"); absent(item, "totalScore"); }
        }
    }

    private Map<String, String> snapshot() throws Exception {
        Map<String, String> snapshot = new TreeMap<>();
        for (ReviewCaseEntity rc : cases.findAll()) snapshot.put("case:" + rc.getCaseId(), mapper.writeValueAsString(rc));
        for (ReviewSubmissionEntity s : submissions.findAll()) snapshot.put("sub:" + s.getSubmissionId(), mapper.writeValueAsString(s));
        for (AuditTrailEntity e : audit.findAll()) snapshot.put("audit:" + e.getId(), mapper.writeValueAsString(e));
        return snapshot;
    }
}
