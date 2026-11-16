package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * D-045 第 1、3 點（案件版本欄位的樂觀鎖）與 D-037 第 1、5、6 點（案件 updatedAt）：
 * 以 Flyway 建的 schema 與 ReviewCaseRepositoryImpl 驗證。
 */
@SpringBootTest
class ReviewCaseVersionAndUpdatedAtTest {

    /** 可手動調整的時鐘，用來驗證時間不倒退。 */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-16T10:00:00Z");
        void set(String instant) { now = Instant.parse(instant); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @TestConfiguration
    static class ClockConfig {
        @Bean @Primary MutableClock mutableClock() { return new MutableClock(); }
    }

    @Autowired private ReviewCaseRepository repository;
    @Autowired private ReviewCaseJpaRepository jpa;
    @Autowired private MutableClock clock;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private ReviewSubmissionRepository submissions;
    @Autowired private ReviewSubmissionJpaRepository submissionJpa;

    @BeforeEach
    void clean() {
        submissionJpa.deleteAll();
        jpa.deleteAll();
        clock.set("2026-11-16T10:00:00Z");
    }

    private ReviewCase newCase(String id) {
        return ReviewCase.createFromSampling(id, "b1", "p1", "H001", "SK-" + id, MedicalRecordType.OUTPATIENT);
    }

    private Instant updatedAt(String id) {
        return jpa.findById(id).orElseThrow().getUpdatedAt();
    }

    @Test
    void newCaseStartsAtVersionZeroWithCreationTime() {
        repository.save(newCase("c1"));

        ReviewCaseEntity saved = jpa.findById("c1").orElseThrow();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getUpdatedAt()).isEqualTo(Instant.parse("2026-11-16T10:00:00Z"));
    }

    @Test
    void businessChangeAdvancesTimeAndVersion_noChangeDoesNot() {
        repository.save(newCase("c1"));
        clock.set("2026-11-16T11:00:00Z");

        ReviewCase unchanged = repository.findById("c1").orElseThrow();
        repository.save(unchanged);
        assertThat(updatedAt("c1")).isEqualTo(Instant.parse("2026-11-16T10:00:00Z"));
        assertThat(jpa.findById("c1").orElseThrow().getVersion()).isZero();

        ReviewCase changed = repository.findById("c1").orElseThrow();
        changed.assignReviewer("doctor", null);
        repository.save(changed);
        assertThat(updatedAt("c1")).isEqualTo(Instant.parse("2026-11-16T11:00:00Z"));
        assertThat(jpa.findById("c1").orElseThrow().getVersion()).isEqualTo(1L);
    }

    @Test
    void timeNeverMovesBackwardsWhenTheClockDoes() {
        repository.save(newCase("c1"));
        clock.set("2026-11-16T09:00:00Z");

        ReviewCase changed = repository.findById("c1").orElseThrow();
        changed.assignReviewer("doctor", null);
        repository.save(changed);

        assertThat(updatedAt("c1")).isEqualTo(Instant.parse("2026-11-16T10:00:00Z"));
    }

    @Test
    void legacyCaseWithoutTimeStaysNullUntilItsFirstRealChange() {
        ReviewCaseEntity legacy = new ReviewCaseEntity();
        legacy.setCaseId("legacy");
        legacy.setBatchId("b1");
        legacy.setProjectId("p1");
        legacy.setStatus(CaseStatus.SELECTED_PENDING_ASSIGNMENT);
        jpa.save(legacy);

        repository.save(repository.findById("legacy").orElseThrow());
        assertThat(updatedAt("legacy")).isNull();

        ReviewCase changed = repository.findById("legacy").orElseThrow();
        changed.assignReviewer("doctor", null);
        repository.save(changed);
        assertThat(updatedAt("legacy")).isEqualTo(Instant.parse("2026-11-16T10:00:00Z"));
    }

    @Test
    void staleWriteFailsAndKeepsTheFirstWritersData() {
        repository.save(newCase("c1"));
        TransactionTemplate outer = new TransactionTemplate(transactions);
        TransactionTemplate inner = new TransactionTemplate(transactions);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            ReviewCase stale = repository.findById("c1").orElseThrow();
            inner.executeWithoutResult(s -> {
                ReviewCase first = repository.findById("c1").orElseThrow();
                first.assignReviewer("first-doctor", null);
                repository.save(first);
            });
            stale.cancel("stale writer");
            repository.save(stale);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        ReviewCaseEntity persisted = jpa.findById("c1").orElseThrow();
        assertThat(persisted.getReviewerId()).isEqualTo("first-doctor");
        assertThat(persisted.getStatus()).isEqualTo(CaseStatus.PENDING_INITIAL_REVIEW);
        assertThat(persisted.getCancelReason()).isNull();
    }

    /**
     * 只寫提交時也要檢查案件版本，即使時鐘倒退、updatedAt 維持不變（案件沒有其他欄位變更）。
     * 否則兩個同時的草稿儲存都會成功，後寫者覆蓋先寫者（D-045）。
     */
    @Test
    void concurrentSubmissionOnlyWritesConflictEvenWhenTheTimeCannotAdvance() {
        repository.save(newCase("c1"));
        ReviewSubmission draft = ReviewSubmission.createRereviewDraft("s1", "c1", "doctor");
        submissions.save(draft);
        clock.set("2026-11-16T09:00:00Z");
        TransactionTemplate outer = new TransactionTemplate(transactions);
        TransactionTemplate inner = new TransactionTemplate(transactions);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            repository.findById("c1").orElseThrow();
            ReviewSubmission stale = submissions.findById("s1").orElseThrow();
            inner.executeWithoutResult(s -> {
                ReviewSubmission first = submissions.findById("s1").orElseThrow();
                first.updateDraft(List.of(AnswerItem.ofScored("Q", "first", BigDecimal.ONE)), BigDecimal.ONE, "first");
                submissions.save(first);
            });
            stale.updateDraft(List.of(AnswerItem.ofScored("Q", "stale", BigDecimal.TEN)), BigDecimal.TEN, "stale");
            submissions.save(stale);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(submissionJpa.findById("s1").orElseThrow().getComment()).isEqualTo("first");
        assertThat(updatedAt("c1")).isEqualTo(Instant.parse("2026-11-16T10:00:00Z"));
    }
}
