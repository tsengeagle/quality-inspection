package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailEntity;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DD-03 第 5 點、D-054（TS-DD03-002）：初審固定與初審 revision 交錯時，只有一方成功；失敗方整筆回滾，
 * 不留下提交、固定欄位或稽核紀錄。以真實 JPA 與兩個獨立交易驗證案件版本檢查（D-045）。
 */
@SpringBootTest
class FixInitialReviewConcurrencyTest {

    @Autowired private FixInitialReviewUseCase fix;
    @Autowired private ReviseInitialReviewUseCase revise;
    @Autowired private ReviewCaseRepository caseRepository;
    @Autowired private ReviewCaseJpaRepository cases;
    @Autowired private ReviewSubmissionJpaRepository submissions;
    @Autowired private AuditTrailJpaRepository audit;
    @Autowired private ScoreSheetRepository scoreSheets;
    @Autowired private ReviewProjectRepository projects;
    @Autowired private PlatformTransactionManager transactions;

    private TransactionTemplate outer;
    private TransactionTemplate inner;

    @BeforeEach
    void seed() {
        audit.deleteAll();
        submissions.deleteAll();
        cases.deleteAll();
        if (scoreSheets.findById("ss").isEmpty()) {
            ScoreSheetDefinition sheet = ScoreSheetDefinition.createDraft("ss", "p");
            ScoreSheetSection section = new ScoreSheetSection("section", "Section");
            section.addItem(new ScoreItem("Q", "Q", "Question", ScoreType.BOOLEAN,
                    BigDecimal.TEN, BigDecimal.ONE, RequiredRule.ALWAYS_REQUIRED, null));
            sheet.addSection(section);
            sheet.publishVersion();
            scoreSheets.save(sheet);
        }
        projects.save(ReviewProject.reconstitute("p", "H001", "Project", null, ScoreSheetVersionId.of("ss", 1), null,
                ProjectStatus.ACTIVE, 1, true, new BigDecimal("100"), new BigDecimal("0.5"), new BigDecimal("0.5")));

        ReviewCaseEntity rc = new ReviewCaseEntity();
        rc.setCaseId("c1");
        rc.setBatchId("b1");
        rc.setProjectId("p");
        rc.setHospitalId("H001");
        rc.setStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);
        rc.setReviewerId("doctor");
        rc.setReviewerAssignedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        rc.setLatestInitialSubmissionId("i1");
        rc.setScoreSheetVersionRef(ScoreSheetVersionId.of("ss", 1));
        rc.setInitialReviewScore(BigDecimal.TEN);
        rc.setFinalScore(BigDecimal.TEN);
        cases.save(rc);

        ReviewSubmissionEntity sub = new ReviewSubmissionEntity();
        sub.setSubmissionId("i1");
        sub.setCaseId("c1");
        sub.setStage(ReviewStage.INITIAL);
        sub.setReviewerId("doctor");
        sub.setStatus(SubmissionStatus.SUBMITTED);
        sub.setRevisionNo(1);
        sub.setAnswerSet(List.of(AnswerItem.ofScored("Q", "true", BigDecimal.TEN)));
        sub.setTotalScore(BigDecimal.TEN);
        sub.setBasedOnScoreSheetVersionId(ScoreSheetVersionId.of("ss", 1));
        sub.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        sub.setSubmittedAt(sub.getCreatedAt());
        submissions.save(sub);

        outer = new TransactionTemplate(transactions);
        inner = new TransactionTemplate(transactions);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private static ActorContext doctor() {
        return new ActorContext("doctor", "Doctor", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
    }

    private List<String> auditActions() {
        return audit.findAll().stream().map(AuditTrailEntity::getAction).sorted().collect(Collectors.toList());
    }

    @Test
    void fixCommittedFirst_staleRevisionFailsAndLeavesNothingBehind() {
        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            caseRepository.findById("c1").orElseThrow(); // 讀到尚未固定的案件
            inner.executeWithoutResult(s -> fix.execute(doctor(), "c1", "i1"));
            revise.execute(doctor(), "c1", "i1", List.of(new AnswerInput("Q", "false")), "stale revision");
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
        assertThat(rc.getInitialFixedSubmissionId()).isEqualTo("i1");
        assertThat(rc.getLatestInitialSubmissionId()).isEqualTo("i1");
        assertThat(submissions.findAll()).extracting(ReviewSubmissionEntity::getSubmissionId).containsExactly("i1");
        assertThat(submissions.findById("i1").orElseThrow().getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(auditActions()).containsExactly("INITIAL_REVIEW_FIXED");
    }

    /** revision 先提交：固定重新讀到已被取代的提交，在領域規則就以 CONFLICT 拒絕（D-054 第 6 點）。 */
    @Test
    void revisionCommittedFirst_fixRereadsTheSupersededSubmissionAndIsConflict() {
        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            caseRepository.findById("c1").orElseThrow(); // 讀到有效版本仍是 i1 的案件
            inner.executeWithoutResult(s ->
                    revise.execute(doctor(), "c1", "i1", List.of(new AnswerInput("Q", "false")), "revision"));
            fix.execute(doctor(), "c1", "i1");
        })).isInstanceOfSatisfying(com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("CONFLICT"));

        assertRevisionWonAndNothingWasFixed();
    }

    /** 案件與提交都在 revision 提交前讀入（兩者都是舊的）：領域規則看不出來，由案件版本檢查在 commit 時拒絕（D-045）。 */
    @Test
    void revisionCommittedFirst_fullyStaleFixFailsOnTheCaseVersion() {
        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            caseRepository.findById("c1").orElseThrow();
            submissions.findById("i1").orElseThrow(); // 提交也停在 revision 之前的狀態
            inner.executeWithoutResult(s ->
                    revise.execute(doctor(), "c1", "i1", List.of(new AnswerInput("Q", "false")), "revision"));
            fix.execute(doctor(), "c1", "i1");
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertRevisionWonAndNothingWasFixed();
    }

    private void assertRevisionWonAndNothingWasFixed() {
        ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
        assertThat(rc.getInitialFixedAt()).isNull();
        assertThat(rc.getInitialFixedBy()).isNull();
        assertThat(rc.getInitialFixedSubmissionId()).isNull();
        assertThat(rc.getLatestInitialSubmissionId()).isNotEqualTo("i1");
        assertThat(submissions.findById("i1").orElseThrow().getStatus()).isEqualTo(SubmissionStatus.SUPERSEDED);
        assertThat(auditActions()).doesNotContain("INITIAL_REVIEW_FIXED");
    }
}
