package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignment;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ReviewCaseRepositoryImpl implements ReviewCaseRepository {

    private final ReviewCaseJpaRepository jpa;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    public ReviewCaseRepositoryImpl(ReviewCaseJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    /**
     * 把 domain 的狀態寫回這個案件的 entity。既有案件先載入 entity 再覆寫欄位，保留 Hibernate 的版本號：
     * 交易內載入的是同一個受管 entity，別人先提交了更新，flush 時就以樂觀鎖失敗（D-045）。
     * 業務欄位有變更（或是新案件）才推進 updatedAt（D-037）；沒有變更不寫入。
     */
    @Override
    public void save(ReviewCase reviewCase) {
        jpa.save(toEntity(reviewCase));
    }

    @Override
    public void saveAll(List<ReviewCase> cases) {
        jpa.saveAll(cases.stream().map(this::toEntity).collect(Collectors.toList()));
    }

    /**
     * 只寫入提交（例如草稿儲存）時，由提交的 repository 呼叫，推進所屬案件的 updatedAt（D-037 第 1 點），
     * 並強制遞增案件版本（{@code OPTIMISTIC_FORCE_INCREMENT}）：即使時鐘倒退使 updatedAt 不變、案件沒有欄位變更，
     * commit 時仍以版本檢查同一案件的並行寫入（D-045）。提交本身沒有版本欄位，靠案件版本保護。
     * 加入呼叫端的交易；沒有交易時自己開一個。案件不存在時不做事。
     */
    @Transactional
    public void touch(String caseId) {
        ReviewCaseEntity e = entityManager.find(ReviewCaseEntity.class, caseId);
        if (e == null) {
            return;
        }
        e.setUpdatedAt(advance(e.getUpdatedAt()));
        entityManager.lock(e, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    /** 不倒退：取目前值與現在時間較晚者（D-037 第 5 點）。精度到微秒，與 Oracle TIMESTAMP 預設相同。 */
    private Instant advance(Instant current) {
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        return current == null || now.isAfter(current) ? now : current;
    }

    @Override
    public Optional<ReviewCase> findById(String caseId) {
        return jpa.findById(caseId).map(this::toDomain);
    }

    private ReviewCase toDomain(ReviewCaseEntity e) {
        ReviewerAssignment assignment = null;
        if (e.getReviewerId() != null && e.getReviewerAssignedAt() != null) {
            assignment = new ReviewerAssignment(e.getReviewerId(), e.getReviewerAssignedAt());
        }

        ReviewerAssignment rereviewerAssignment = null;
        if (e.getRereviewerId() != null && e.getRereviewerAssignedAt() != null) {
            rereviewerAssignment = new ReviewerAssignment(e.getRereviewerId(), e.getRereviewerAssignedAt());
        }

        MedicalRecordType mrt = null;
        if (e.getMedicalRecordType() != null) {
            mrt = MedicalRecordType.valueOf(e.getMedicalRecordType());
        }

        return ReviewCase.reconstitute(
                e.getCaseId(), e.getBatchId(), e.getProjectId(), e.getHospitalId(),
                e.getSourceCaseKey(), mrt,
                e.getStatus(), assignment, e.getDueAt(),
                e.getCancelReason(), e.getReturnReason(), e.getCancelledAt(), e.getClosedAt(),
                e.isRereviewRequired(), e.getFinalScore(), e.getFinalConclusion(),
                e.getLatestInitialSubmissionId(), e.getRuleSnapshotRef(), e.getScoreSheetVersionRef(),
                rereviewerAssignment, e.getRereviewerDueAt(), e.getInitialReviewScore(),
                e.getLatestRereviewSubmissionId(),
                e.getInitialFixedAt(), e.getInitialFixedBy(), e.getInitialFixedSubmissionId());
    }

    private ReviewCaseEntity toEntity(ReviewCase rc) {
        ReviewCaseEntity e = jpa.findById(rc.getCaseId()).orElse(null);
        boolean isNew = e == null;
        if (isNew) {
            e = new ReviewCaseEntity();
        }
        Object[] before = isNew ? null : businessState(e);
        apply(rc, e);
        if (isNew || !Arrays.equals(before, businessState(e))) {
            e.setUpdatedAt(advance(e.getUpdatedAt()));
        }
        return e;
    }

    private static void apply(ReviewCase rc, ReviewCaseEntity e) {
        e.setCaseId(rc.getCaseId());
        e.setBatchId(rc.getBatchId());
        e.setProjectId(rc.getProjectId());
        e.setHospitalId(rc.getHospitalId());
        e.setSourceCaseKey(rc.getSourceCaseKey());
        e.setMedicalRecordType(rc.getMedicalRecordType() != null ? rc.getMedicalRecordType().name() : null);
        e.setStatus(rc.getStatus());
        e.setReviewerId(rc.getReviewerAssignment() != null ? rc.getReviewerAssignment().getReviewerId() : null);
        e.setReviewerAssignedAt(rc.getReviewerAssignment() != null ? rc.getReviewerAssignment().getAssignedAt() : null);
        e.setDueAt(rc.getDueAt());
        e.setCancelReason(rc.getCancelReason());
        e.setReturnReason(rc.getReturnReason());
        e.setCancelledAt(rc.getCancelledAt());
        e.setClosedAt(rc.getClosedAt());
        e.setRereviewRequired(rc.isRereviewRequired());
        e.setFinalScore(rc.getFinalScore());
        e.setFinalConclusion(rc.getFinalConclusion());
        e.setLatestInitialSubmissionId(rc.getLatestInitialSubmissionId());
        e.setRuleSnapshotRef(rc.getRuleSnapshotRef());
        e.setScoreSheetVersionRef(rc.getScoreSheetVersionRef());
        e.setRereviewerId(rc.getRereviewerAssignment() != null ? rc.getRereviewerAssignment().getReviewerId() : null);
        e.setRereviewerAssignedAt(rc.getRereviewerAssignment() != null ? rc.getRereviewerAssignment().getAssignedAt() : null);
        e.setRereviewerDueAt(rc.getRereviewerDueAt());
        e.setInitialReviewScore(rc.getInitialReviewScore());
        e.setLatestRereviewSubmissionId(rc.getLatestRereviewSubmissionId());
        e.setInitialFixedAt(rc.getInitialFixedAt());
        e.setInitialFixedBy(rc.getInitialFixedBy());
        e.setInitialFixedSubmissionId(rc.getInitialFixedSubmissionId());
    }

    /** 所有業務欄位（不含 version 與 updatedAt）。數值以 stripTrailingZeros 比較，避免 10 與 10.00 被當成變更。 */
    private static Object[] businessState(ReviewCaseEntity e) {
        return new Object[] {
                e.getCaseId(), e.getBatchId(), e.getProjectId(), e.getHospitalId(), e.getSourceCaseKey(),
                e.getMedicalRecordType(), e.getStatus(), e.getReviewerId(), e.getReviewerAssignedAt(), e.getDueAt(),
                e.getCancelReason(), e.getReturnReason(), e.getCancelledAt(), e.getClosedAt(), e.isRereviewRequired(),
                number(e.getFinalScore()), e.getFinalConclusion(), e.getLatestInitialSubmissionId(),
                e.getRuleSnapshotRef(), e.getScoreSheetVersionRef(), e.getRereviewerId(), e.getRereviewerAssignedAt(),
                e.getRereviewerDueAt(), number(e.getInitialReviewScore()), e.getLatestRereviewSubmissionId(),
                e.getInitialFixedAt(), e.getInitialFixedBy(), e.getInitialFixedSubmissionId()
        };
    }

    private static Object number(java.math.BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }
}
