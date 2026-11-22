package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.application.common.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.*;
import com.tzuchi.his.quality.inspection.domain.project.*;
import com.tzuchi.his.quality.inspection.domain.reviewcase.*;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.*;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.*;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RecordApplicationEvents
class UpdatedAtContractIntegrationTest {
    @Autowired private SaveInitialReviewDraftUseCase saveDraft;
    @Autowired private SubmitInitialReviewUseCase submit;
    @Autowired private ReviseInitialReviewUseCase revise;
    @Autowired private ReviewCaseJpaRepository cases;
    @Autowired private ReviewSubmissionJpaRepository submissions;
    @Autowired private ScoreSheetRepository scores;
    @Autowired private ReviewProjectRepository projects;
    @Autowired private AuditTrailJpaRepository audit;
    @Autowired private ObjectMapper mapper;
    @Autowired private ApplicationEvents events;
    @Autowired private ReviewCaseRepositoryImpl caseRepository;
    @Autowired private ReviewSubmissionRepositoryImpl submissionRepository;
    @Autowired private StartInitialReviewUseCase start;
    @Autowired private ReturnInitialReviewUseCase returnReview;
    @Autowired private ReopenInitialReviewUseCase reopen;

    @BeforeEach
    void seed() {
        audit.deleteAll();
        submissions.deleteAll();
        cases.deleteAll();
        if (!scores.findById("ss").isPresent()) {
            ScoreSheetDefinition sheet = ScoreSheetDefinition.createDraft("ss", "p");
            ScoreSheetSection section = new ScoreSheetSection("section", "Section");
            section.addItem(new ScoreItem("Q", "Q", "Question", ScoreType.BOOLEAN,
                    BigDecimal.TEN, BigDecimal.ONE, RequiredRule.ALWAYS_REQUIRED, null));
            sheet.addSection(section);
            sheet.publishVersion();
            scores.save(sheet);
        }
        projects.save(ReviewProject.reconstitute("p", "H001", "Project", null, "ss", null,
                ProjectStatus.ACTIVE, 1, true, new BigDecimal("100"),
                new BigDecimal("0.5"), new BigDecimal("0.5")));
        ReviewCaseEntity rc = new ReviewCaseEntity();
        rc.setCaseId("c1");
        rc.setBatchId("batch");
        rc.setProjectId("p");
        rc.setHospitalId("H001");
        rc.setStatus(CaseStatus.INITIAL_REVIEW_IN_PROGRESS);
        rc.setReviewerId("doctor");
        rc.setReviewerAssignedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        rc.setLatestInitialSubmissionId("s1");
        rc.setScoreSheetVersionRef(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of("ss", 1));
        rc.setInitialReviewScore(BigDecimal.TEN);
        rc.setFinalScore(BigDecimal.TEN);
        cases.save(rc);
        ReviewSubmissionEntity sub = new ReviewSubmissionEntity();
        sub.setSubmissionId("s1");
        sub.setCaseId("c1");
        sub.setStage(ReviewStage.INITIAL);
        sub.setReviewerId("doctor");
        sub.setStatus(SubmissionStatus.DRAFT);
        sub.setRevisionNo(1);
        sub.setAnswerSet(Collections.singletonList(AnswerItem.ofScored("Q", "true", BigDecimal.TEN)));
        sub.setTotalScore(BigDecimal.TEN);
        sub.setComment("original");
        sub.setBasedOnScoreSheetVersionId(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of("ss", 1));
        sub.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        sub.setUpdatedAt(sub.getCreatedAt());
        submissions.save(sub);
    }

    private ActorContext actor(String hospital) {
        return new ActorContext("doctor", "Doctor", EnumSet.of(DomainRole.REVIEWER), Optional.of(hospital));
    }

    private List<AnswerInput> answers(String value) {
        return Collections.singletonList(new AnswerInput("Q", value));
    }


    @Autowired private SubmitRereviewUseCase rereviewSubmit;
    @Autowired private ReviseRereviewUseCase rereviewRevise;
    @Autowired private SaveRereviewDraftUseCase rereviewSave;
    @Autowired private CancelReviewCaseUseCase cancel;
    @Autowired private org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired private javax.persistence.EntityManagerFactory emf;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactions;

    private ActorContext context(DomainRole role, String hospital, String user) {
        return new ActorContext(user, user, EnumSet.of(role), Optional.of(hospital));
    }

    private void rereviewFixture() {
        ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
        rc.setStatus(CaseStatus.REREVIEW_IN_PROGRESS);
        rc.setReviewerId("initial-doctor"); // Preserve D-017 distinct initial/rereview identities.
        rc.setRereviewerId("doctor"); rc.setRereviewerAssignedAt(LocalDateTime.of(2026,1,1,0,0));
        rc.setLatestRereviewSubmissionId("s1"); cases.save(rc);
        ReviewSubmissionEntity sub = submissions.findById("s1").orElseThrow();
        sub.setStage(ReviewStage.REREVIEW); submissions.save(sub);
    }

    private SaveDraftResult save(boolean rereview, ActorContext actor, boolean changed) {
        if (!rereview) return saveDraft.execute(actor, "c1", answers(changed ? "false" : "true"), changed ? "changed" : "original");
        com.tzuchi.his.quality.inspection.application.reviewcase.command.SaveRereviewDraftCommand cmd =
                new com.tzuchi.his.quality.inspection.application.reviewcase.command.SaveRereviewDraftCommand();
        cmd.setCaseId("c1"); cmd.setSubmissionId("s1");
        cmd.setAnswers(List.of(AnswerItem.ofScored("Q", changed ? "false" : "true", BigDecimal.TEN)));
        cmd.setTotalScore(BigDecimal.TEN); cmd.setComment(changed ? "changed" : "original");
        return rereviewSave.execute(actor, cmd);
    }

    private com.fasterxml.jackson.databind.JsonNode metadata() throws Exception {
        String user = mapper.writeValueAsString(Map.of("user", Map.of("user_id", "admin", "user_name", "Admin",
                "hospital_id", "H001", "user_role", List.of(Map.of("role_name", "system_admin", "enabled", true)))));
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String token = enc.encodeToString("{\"alg\":\"RS512\",\"typ\":\"JWT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(mapper.writeValueAsBytes(Map.of("tz-user", user))) + ".test-sig";
        String body = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/review-cases/c1")
                .header("api-key", token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data");
    }

    private java.time.Instant requireTime() throws Exception {
        com.fasterxml.jackson.databind.JsonNode value = metadata().path("updatedAt");
        assertTrue(value.isTextual(), "D-037: successful business update must expose persisted UTC updatedAt; got " + value);
        assertEquals(java.time.ZoneOffset.UTC, java.time.OffsetDateTime.parse(value.asText()).getOffset(), "D-037 UTC timestamp");
        return java.time.Instant.parse(value.asText());
    }

    private String snapshot() throws Exception {
        return mapper.writeValueAsString(List.of(cases.findAll(), submissions.findAll(), audit.findAll()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void identicalDraftMustNotWriteTimestampOrAudit(boolean rereview) throws Exception {
        if (rereview) rereviewFixture();
        String before = snapshot(); long eventCount = events.stream(DomainEvent.class).count();
        org.hibernate.stat.Statistics stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics(); stats.clear();
        // Error type/status is pending. Check the accepted side-effect prohibition without inventing it.
        try { save(rereview, context(rereview ? DomainRole.REREVIEWER : DomainRole.REVIEWER, "H001", "doctor"), false); }
        catch (RuntimeException rejection) { /* Side effects are asserted independently below. */ }
        long updates = stats.getEntityUpdateCount(), inserts = stats.getEntityInsertCount();
        assertAll(() -> assertEquals(before, snapshot(), "No-change persisted data"),
                () -> assertEquals(0, updates, "No-change must not update entities"),
                () -> assertEquals(0, inserts, "No-change must not insert Audit/entities"),
                () -> assertEquals(eventCount, events.stream(DomainEvent.class).count(), "No new Draft Save Event"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void changedDraftMustUpdateCaseTime(boolean rereview) throws Exception {
        if (rereview) rereviewFixture();
        save(rereview, context(rereview ? DomainRole.REREVIEWER : DomainRole.REVIEWER, "H001", "doctor"), true);
        assertEquals("changed", submissions.findById("s1").orElseThrow().getComment());
        requireTime();
    }

    @ParameterizedTest
    @ValueSource(strings = {"initialScope", "rereviewScope", "initialOwner", "rereviewOwner", "initialTerminal", "rereviewTerminal", "initialPointer", "rereviewPointer", "initialRole", "rereviewRole"})
    void identicalContentMustStillRunGuards(String scenario) throws Exception {
        boolean rr = scenario.startsWith("rereview"); if (rr) rereviewFixture();
        ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
        if (scenario.endsWith("Terminal")) rc.setStatus(CaseStatus.CLOSED);
        if (scenario.endsWith("Pointer")) { if (rr) rc.setLatestRereviewSubmissionId("stale"); else rc.setLatestInitialSubmissionId("stale"); }
        cases.save(rc);
        ActorContext who = context(scenario.endsWith("Role") ? DomainRole.AUDITOR : rr ? DomainRole.REREVIEWER : DomainRole.REVIEWER,
                scenario.endsWith("Scope") ? "H002" : "H001", scenario.endsWith("Owner") ? "stranger" : "doctor");
        String before = snapshot(); long eventCount = events.stream(DomainEvent.class).count();
        RuntimeException failure = assertThrows(RuntimeException.class, () -> save(rr, who, false));
        assertTrue(failure instanceof AccessDeniedException || failure instanceof RoleDeniedException
                || failure instanceof DomainStateConflictException || failure instanceof DomainRuleViolationException);
        assertEquals(before, snapshot()); assertEquals(eventCount, events.stream(DomainEvent.class).count());
    }

    @Test
    void newCaseMustInitializeTime() throws Exception {
        submissions.deleteAll(); cases.deleteAll();
        caseRepository.save(ReviewCase.createFromSampling("c1", "batch", "p", "H001", "source", MedicalRecordType.OUTPATIENT));
        requireTime();
    }

    @Test
    void submitMustUpdateCaseTime() throws Exception {
        submit.execute(actor("H001"), "c1"); requireTime();
    }

    @Test
    void revisionMustUpdateCaseTime() throws Exception {
        submit.execute(actor("H001"), "c1");
        revise.execute(actor("H001"), "c1", "s1", answers("false"), "revision"); requireTime();
    }


    @Test
    void rereviewSubmitMustUpdateCaseTime_withoutRepairingD032() throws Exception {
        rereviewFixture();
        com.tzuchi.his.quality.inspection.application.reviewcase.command.SubmitRereviewCommand cmd =
                new com.tzuchi.his.quality.inspection.application.reviewcase.command.SubmitRereviewCommand();
        cmd.setCaseId("c1"); cmd.setSubmissionId("s1"); cmd.setComment("submitted");
        rereviewSubmit.execute(context(DomainRole.REREVIEWER, "H001", "doctor"), cmd);
        requireTime();
    }

    @Test
    void rereviewRevisionMustUpdateCaseTime() throws Exception {
        rereviewFixture();
        // Seed the existing legal nonterminal revision precondition; do not submit/close and reopen it.
        ReviewSubmissionEntity previous = submissions.findById("s1").orElseThrow();
        previous.setStatus(SubmissionStatus.SUBMITTED); previous.setSubmittedAt(previous.getCreatedAt()); submissions.save(previous);
        com.tzuchi.his.quality.inspection.application.reviewcase.command.ReviseRereviewCommand cmd =
                new com.tzuchi.his.quality.inspection.application.reviewcase.command.ReviseRereviewCommand();
        cmd.setCaseId("c1"); cmd.setSubmissionId("s1"); cmd.setAnswers(List.of(AnswerItem.ofRaw("Q", "false"))); cmd.setComment("revision");
        rereviewRevise.execute(context(DomainRole.REREVIEWER, "H001", "doctor"), cmd);
        assertEquals(SubmissionStatus.SUPERSEDED, submissions.findById("s1").orElseThrow().getStatus());
        requireTime();
    }

    @Test
    void caseOnlyChangeMustUpdateTime() throws Exception {
        cancel.execute(context(DomainRole.HOSPITAL_MANAGER, "H001", "manager"), "c1", "cancel");
        assertEquals(CaseStatus.CANCELLED, cases.findById("c1").orElseThrow().getStatus()); requireTime();
    }

    @Test
    void failedTransactionMustRollbackBusinessData_controlForPendingCaseTimestamp() throws Exception {
        String before = snapshot();
        org.springframework.transaction.support.TransactionTemplate tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            save(false, actor("H001"), true);
            throw new IllegalStateException("injected failure after real use case writes");
        }));
        assertEquals(before, snapshot());
        assertTrue(metadata().path("updatedAt").isNull(), "Existing legacy has no trusted case time");
    }


    @Test
    void legacyNullAndEightFieldWhitelistRemain() throws Exception {
        com.fasterxml.jackson.databind.JsonNode data = metadata(); Set<String> keys = new HashSet<>(); data.fieldNames().forEachRemaining(keys::add);
        assertEquals(Set.of("caseId", "caseStatus", "projectName", "hospitalId", "reviewerId", "rereviewerId", "updatedAt", "latestRevisionSummary"), keys);
        assertTrue(data.path("updatedAt").isNull()); assertFalse(data.has("lifecycleLatestStage"));
    }

    private String draftAuth(boolean rr, String scenario) throws Exception {
        String role = scenario.endsWith("Role") ? "auditor" : rr ? "rereviewer" : "reviewer";
        String user = scenario.endsWith("Owner") ? "stranger" : "doctor";
        String hospital = scenario.endsWith("Scope") ? "H002" : "H001";
        String claims = mapper.writeValueAsString(Map.of("user", Map.of("user_id", user, "user_name", user,
                "hospital_id", hospital, "user_role", List.of(Map.of("role_name", role, "enabled", true)))));
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString("{\"alg\":\"RS512\",\"typ\":\"JWT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(mapper.writeValueAsBytes(Map.of("tz-user", claims))) + ".test-sig";
    }

    private org.springframework.test.web.servlet.MvcResult postDraft(boolean rr, String scenario) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("answers", List.of(Map.of("itemId", "Q", "answerValue", "false")));
        payload.put("comment", "HTTP baseline");
        if (rr) { payload.put("submissionId", "s1"); payload.put("totalScore", new BigDecimal("3")); }
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/review-cases/c1/" + (rr ? "rereview" : "initial-review") + "/draft")
                .header("api-key", draftAuth(rr, scenario)).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(payload))).andReturn();
    }

    private void prepareIdenticalHttpDraft(boolean rr) throws Exception {
        if (rr) rereviewFixture();
        org.springframework.test.web.servlet.MvcResult response = postDraft(rr, "valid");
        assertEquals(200, response.getResponse().getStatus(), "Changed draft must still save");
        com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(response.getResponse().getContentAsString());
        assertTrue(json.path("success").asBoolean());
        assertEquals("s1", json.path("data").path("submissionId").asText());
        ReviewSubmissionEntity persisted = submissions.findById("s1").orElseThrow();
        assertEquals("HTTP baseline", persisted.getComment());
        assertEquals("false", persisted.getAnswerSet().get(0).getAnswerValue());
        if (rr) assertEquals(0, new BigDecimal("3").compareTo(persisted.getTotalScore()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void identicalHttpDraftMustReject409NoChangesWithoutSideEffects(boolean rr) throws Exception {
        // A completed real HTTP save supplies the canonical persisted comparison baseline.
        prepareIdenticalHttpDraft(rr);
        String before = snapshot(); long eventCount = events.stream(DomainEvent.class).count();
        org.hibernate.stat.Statistics stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics(); stats.clear();
        org.springframework.test.web.servlet.MvcResult response = postDraft(rr, "valid");
        long updates = stats.getEntityUpdateCount(), inserts = stats.getEntityInsertCount();
        com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(response.getResponse().getContentAsString());
        assertAll(() -> assertEquals(409, response.getResponse().getStatus()),
                () -> assertEquals("NO_CHANGES", json.path("error").path("code").asText()),
                () -> assertFalse(json.path("success").asBoolean(true)),
                () -> assertTrue(json.path("data").isMissingNode() || json.path("data").isNull(), "No success save result"),
                () -> assertEquals(before, snapshot(), "Reloaded Case/Submission/Audit must remain unchanged"),
                () -> assertEquals(eventCount, events.stream(DomainEvent.class).count()),
                () -> assertEquals(0, updates, "No entity updates"),
                () -> assertEquals(0, inserts, "No Audit/entity inserts"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void changedHttpDraftMustKeepLegalSave(boolean rr) throws Exception {
        prepareIdenticalHttpDraft(rr);
    }

    @ParameterizedTest
    @ValueSource(strings = {"initialScope", "rereviewScope", "initialOwner", "rereviewOwner", "initialTerminal", "rereviewTerminal", "initialPointer", "rereviewPointer", "initialRole", "rereviewRole"})
    void identicalHttpPayloadMustRunOriginalGuardsBeforeNoChanges(String scenario) throws Exception {
        boolean rr = scenario.startsWith("rereview"); prepareIdenticalHttpDraft(rr);
        ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
        if (scenario.endsWith("Terminal")) rc.setStatus(CaseStatus.CLOSED);
        if (scenario.endsWith("Pointer")) { if (rr) rc.setLatestRereviewSubmissionId("stale"); else rc.setLatestInitialSubmissionId("stale"); }
        cases.save(rc);
        String before = snapshot(); long eventCount = events.stream(DomainEvent.class).count();
        org.springframework.test.web.servlet.MvcResult response = postDraft(rr, scenario);
        com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(response.getResponse().getContentAsString());
        String expected = scenario.endsWith("Role") ? "ROLE_DENIED" : scenario.endsWith("Scope") ? "HOSPITAL_SCOPE_MISMATCH"
                : scenario.endsWith("Owner") ? rr ? "NOT_ASSIGNED_REREVIEWER" : "SUBMISSION_NOT_OWNED_BY_ACTOR"
                : scenario.endsWith("Terminal") ? "CASE_TERMINAL" : "CONFLICT";
        int expectedStatus = scenario.endsWith("Owner") && rr ? 422 : scenario.endsWith("Terminal") || scenario.endsWith("Pointer") ? 409 : 403;
        assertAll(() -> assertEquals(expectedStatus, response.getResponse().getStatus()),
                () -> assertEquals(expected, json.path("error").path("code").asText()),
                () -> assertFalse(json.path("success").asBoolean(true)),
                () -> assertTrue(json.path("data").isNull() || json.path("data").isMissingNode()),
                () -> assertEquals(before, snapshot()),
                () -> assertEquals(eventCount, events.stream(DomainEvent.class).count()));
    }
}
