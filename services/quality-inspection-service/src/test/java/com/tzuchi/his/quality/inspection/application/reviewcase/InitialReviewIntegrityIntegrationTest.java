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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.event.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RecordApplicationEvents
class InitialReviewIntegrityIntegrationTest {
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
    @SpyBean private ReviewCaseRepositoryImpl caseRepository;
    @SpyBean private ReviewSubmissionRepositoryImpl submissionRepository;
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
        projects.save(ReviewProject.reconstitute("p", "H001", "Project", null,
                com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of("ss", 1), null,
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

    static Stream<Arguments> invalidRequests() {
        List<Arguments> args = new ArrayList<>();
        for (String operation : Arrays.asList("draft", "submit", "revision")) {
            for (String scenario : Arrays.asList("wrongHospital", "otherOwner", "stale", "missingPointer",
                    "successor", "superseded", "wrongStatus", "CLOSED", "CANCELLED")) {
                args.add(Arguments.of(operation, scenario));
            }
        }
        for (CaseStatus state : CaseStatus.values()) {
            if (state != CaseStatus.INITIAL_REVIEW_IN_PROGRESS && state != CaseStatus.CLOSED && state != CaseStatus.CANCELLED) {
                args.add(Arguments.of("draft", state.name()));
            }
        }
        for (String operation : Arrays.asList("draft", "submit")) {
            for (String scenario : Arrays.asList("blankPointer", "referenceOtherCase", "referenceRereview", "referenceOtherOwner")) {
                args.add(Arguments.of(operation, scenario));
            }
        }
        args.add(Arguments.of("draftEmpty", "CLOSED"));
        args.add(Arguments.of("draftEmpty", "stale"));
        return args.stream();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidOperation_preservesAllDataAndEvents(String operation, String scenario) throws Exception {
        ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
        ReviewSubmissionEntity sub = submissions.findById("s1").orElseThrow();
        if (operation.equals("revision")) {
            rc.setStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);
            sub.setStatus(SubmissionStatus.SUBMITTED);
            sub.setSubmittedAt(sub.getCreatedAt());
        }
        if (scenario.equals("otherOwner")) sub.setReviewerId("other");
        if (scenario.equals("stale")) rc.setLatestInitialSubmissionId("s-other");
        if (scenario.equals("missingPointer")) rc.setLatestInitialSubmissionId(null);
        if (scenario.equals("blankPointer")) rc.setLatestInitialSubmissionId(" ");
        if (scenario.startsWith("reference")) {
            ReviewSubmissionEntity target = new ReviewSubmissionEntity();
            target.setSubmissionId("s-target");
            target.setCaseId(scenario.equals("referenceOtherCase") ? "other-case" : "c1");
            target.setStage(scenario.equals("referenceRereview") ? ReviewStage.REREVIEW : ReviewStage.INITIAL);
            target.setReviewerId(scenario.equals("referenceOtherOwner") ? "other" : "doctor");
            target.setStatus(SubmissionStatus.DRAFT);
            target.setRevisionNo(1);
            target.setAnswerSet(sub.getAnswerSet());
            target.setTotalScore(sub.getTotalScore());
            target.setComment("referenced target");
            target.setBasedOnScoreSheetVersionId(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of("ss", 1));
            submissions.save(target);
            rc.setLatestInitialSubmissionId(target.getSubmissionId());
        }
        if (scenario.equals("successor")) sub.setSupersededBySubmissionId("s-other");
        if (scenario.equals("superseded")) {
            sub.setStatus(SubmissionStatus.SUPERSEDED);
            sub.setSupersededBySubmissionId("s-other");
        }
        if (scenario.equals("wrongStatus")) sub.setStatus(operation.equals("revision") ? SubmissionStatus.DRAFT : SubmissionStatus.SUBMITTED);
        for (CaseStatus status : CaseStatus.values()) {
            if (scenario.equals(status.name())) rc.setStatus(status);
        }
        cases.save(rc);
        submissions.save(sub);
        Map<String, String> before = snapshot();
        long beforeEvents = events.stream(DomainEvent.class).count();
        long beforeAudit = audit.count();
        ActorContext context = actor(scenario.equals("wrongHospital") ? "H002" : "H001");
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            if (operation.equals("revision")) revise.execute(context, "c1", "s1", answers("false"), "changed");
            else if (operation.equals("submit")) submit.execute(context, "c1");
            else saveDraft.execute(context, "c1", operation.equals("draftEmpty") ? Collections.emptyList() : answers("false"), "changed");
        });
        assertTrue(failure instanceof DomainStateConflictException || failure instanceof AccessDeniedException,
                "Expected an explicit authorization/integrity rejection: " + failure);
        assertEquals(before, snapshot());
        assertEquals(beforeEvents, events.stream(DomainEvent.class).count());
        assertEquals(beforeAudit, audit.count());
    }

    @Test
    void validDraftSubmitAndTwoRevisions_keepChainReferenceAndScoresConsistent() throws Exception {
        saveDraft.execute(actor("H001"), "c1", answers("true"), "saved");
        assertEquals("saved", submissions.findById("s1").orElseThrow().getComment());
        assertEquals("SUBMITTED", submit.execute(actor("H001"), "c1").getStatus());
        RevisionResult r2 = revise.execute(actor("H001"), "c1", "s1", answers("false"), "revision 2");
        assertEquals(r2.getNewSubmissionId(), cases.findById("c1").orElseThrow().getLatestInitialSubmissionId());
        assertEquals(0, BigDecimal.ZERO.compareTo(cases.findById("c1").orElseThrow().getInitialReviewScore()));
        Map<String, String> beforeStaleRetry = snapshot();
        long beforeStaleEvents = events.stream(DomainEvent.class).count();
        long beforeStaleAudit = audit.count();
        assertThrows(DomainStateConflictException.class,
                () -> revise.execute(actor("H001"), "c1", "s1", answers("true"), "stale retry"));
        assertEquals(beforeStaleRetry, snapshot());
        assertEquals(beforeStaleEvents, events.stream(DomainEvent.class).count());
        assertEquals(beforeStaleAudit, audit.count());
        RevisionResult r3 = revise.execute(actor("H001"), "c1", r2.getNewSubmissionId(), answers("true"), "revision 3");
        ReviewSubmissionEntity s1 = submissions.findById("s1").orElseThrow();
        ReviewSubmissionEntity s2 = submissions.findById(r2.getNewSubmissionId()).orElseThrow();
        ReviewSubmissionEntity s3 = submissions.findById(r3.getNewSubmissionId()).orElseThrow();
        assertEquals(3, submissions.count());
        assertEquals(SubmissionStatus.SUPERSEDED, s1.getStatus());
        assertEquals(SubmissionStatus.SUPERSEDED, s2.getStatus());
        assertEquals(SubmissionStatus.SUBMITTED, s3.getStatus());
        assertEquals(r2.getNewSubmissionId(), s1.getSupersededBySubmissionId());
        assertEquals(r3.getNewSubmissionId(), s2.getSupersededBySubmissionId());
        assertEquals(1, s1.getRevisionNo());
        assertEquals(2, s2.getRevisionNo());
        assertEquals(3, s3.getRevisionNo());
        assertEquals("true", s1.getAnswerSet().get(0).getAnswerValue());
        assertEquals("false", s2.getAnswerSet().get(0).getAnswerValue());
        assertEquals("saved", s1.getComment());
        assertEquals("revision 2", s2.getComment());
        for (ReviewSubmissionEntity version : Arrays.asList(s1, s2, s3)) {
            assertEquals("doctor", version.getReviewerId());
            assertEquals(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of("ss", 1), version.getBasedOnScoreSheetVersionId());
        }
        ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
        assertEquals(s3.getSubmissionId(), rc.getLatestInitialSubmissionId());
        assertEquals(0, s3.getTotalScore().compareTo(rc.getInitialReviewScore()));
        assertEquals(0, s3.getTotalScore().compareTo(rc.getFinalScore()));
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(4, audit.count());
    }

    @Test
    void revisionCaseSaveFailure_rollsBackSubmissionChainScoreAndAudit() throws Exception {
        submit.execute(actor("H001"), "c1");
        Map<String, String> before = snapshot();
        long beforeEvents = events.stream(DomainEvent.class).count();
        long beforeAudit = audit.count();
        doThrow(new IllegalStateException("injected case save failure"))
                .when(caseRepository).save(any(ReviewCase.class));
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> revise.execute(actor("H001"), "c1", "s1", answers("false"), "changed"));
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        assertEquals("injected case save failure", cause.getMessage());
        assertEquals(before, snapshot());
        assertEquals(beforeEvents, events.stream(DomainEvent.class).count());
        assertEquals(beforeAudit, audit.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"draft", "submit"})
    void returnReopenStart_selectsCaseReferenceWithoutDependingOnRevisionSort(String operation) throws Exception {
        submissions.deleteAll();
        ReviewCaseEntity pending = cases.findById("c1").orElseThrow();
        pending.setStatus(CaseStatus.PENDING_INITIAL_REVIEW);
        pending.setLatestInitialSubmissionId(null);
        cases.save(pending);
        String s1 = start.execute(actor("H001"), "c1");
        ReviewSubmission first = submissionRepository.findById(s1).orElseThrow();
        first.updateDraft(Collections.singletonList(AnswerItem.ofScored("Q", "true", BigDecimal.TEN)),
                BigDecimal.TEN, "historical");
        submissionRepository.save(first);
        ActorContext manager = new ActorContext("manager", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        returnReview.execute(manager, "c1", "return for review");
        reopen.execute(manager, "c1");
        String s2 = start.execute(actor("H001"), "c1");
        assertNotEquals(s1, s2);
        ReviewSubmission second = submissionRepository.findById(s2).orElseThrow();
        second.updateDraft(Collections.singletonList(AnswerItem.ofScored("Q", "true", BigDecimal.TEN)),
                BigDecimal.TEN, "current");
        submissionRepository.save(second);
        assertEquals(1, submissions.findById(s1).orElseThrow().getRevisionNo());
        assertEquals(1, submissions.findById(s2).orElseThrow().getRevisionNo());
        assertEquals(s2, cases.findById("c1").orElseThrow().getLatestInitialSubmissionId());
        String history = mapper.writeValueAsString(submissions.findById(s1).orElseThrow());
        // Force the old selector to return history: no reliance on a database tie-break.
        doReturn(Optional.of(first)).when(submissionRepository).findLatestByCaseIdAndStage("c1", ReviewStage.INITIAL);
        if (operation.equals("draft")) {
            assertEquals(s2, saveDraft.execute(actor("H001"), "c1", answers("false"), "saved current").getSubmissionId());
            ReviewSubmissionEntity saved = submissions.findById(s2).orElseThrow();
            assertEquals("false", saved.getAnswerSet().get(0).getAnswerValue());
            assertEquals(0, BigDecimal.ZERO.compareTo(saved.getTotalScore()));
            assertEquals("saved current", saved.getComment());
            assertEquals(SubmissionStatus.DRAFT, saved.getStatus());
        } else {
            assertEquals(s2, submit.execute(actor("H001"), "c1").getSubmissionId());
            assertEquals(SubmissionStatus.SUBMITTED, submissions.findById(s2).orElseThrow().getStatus());
        }
        assertEquals(s2, cases.findById("c1").orElseThrow().getLatestInitialSubmissionId());
        assertEquals(history, mapper.writeValueAsString(submissions.findById(s1).orElseThrow()));
        verify(submissionRepository, never()).findLatestByCaseIdAndStage("c1", ReviewStage.INITIAL);
    }

    private Map<String, String> snapshot() throws Exception {
        Map<String, String> state = new TreeMap<>();
        for (ReviewCaseEntity rc : cases.findAll()) state.put("case:" + rc.getCaseId(), mapper.writeValueAsString(rc));
        for (ReviewSubmissionEntity sub : submissions.findAll()) state.put("sub:" + sub.getSubmissionId(), mapper.writeValueAsString(sub));
        return state;
    }
}
