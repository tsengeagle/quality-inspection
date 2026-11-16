package com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess;

import com.tzuchi.his.quality.inspection.application.common.RereviewerHistoryQuery;
import com.tzuchi.his.quality.inspection.application.reviewcase.InitialReviewReaderPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.RereviewerAssignmentHistoryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.RereviewerAssignmentRecord;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.util.Set;

/** D-055 的複審指派歷史與初審讀取者紀錄。寫入方法只在呼叫端的交易中執行（MANDATORY）。 */
@Component
public class RereviewAccessRecordAdapter implements RereviewerAssignmentHistoryPort, InitialReviewReaderPort,
        RereviewerHistoryQuery {

    private final RereviewerAssignmentHistoryJpaRepository history;
    private final InitialReviewReaderJpaRepository readers;

    @PersistenceContext
    private EntityManager entityManager;

    public RereviewAccessRecordAdapter(RereviewerAssignmentHistoryJpaRepository history,
                                       InitialReviewReaderJpaRepository readers) {
        this.history = history;
        this.readers = readers;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(RereviewerAssignmentRecord r) {
        history.save(new RereviewerAssignmentHistoryEntity(r.getCaseId(), r.getRereviewerId(), r.getAssignedAt(),
                r.getAssignedBy(), r.getKind().name()));
    }

    @Override
    public boolean contains(String caseId, String rereviewerId) {
        return history.existsByCaseIdAndRereviewerId(caseId, rereviewerId);
    }

    @Override
    public boolean isComplete(String caseId) {
        ReviewCaseEntity e = entityManager.find(ReviewCaseEntity.class, caseId);
        return e != null && e.isRereviewerHistoryComplete();
    }

    @Override
    public Set<String> findCaseIdsAssignedTo(String rereviewerId) {
        return history.findCaseIdsByRereviewerId(rereviewerId);
    }

    @Override
    public Set<String> findHistoryIncompleteCaseIds() {
        return new java.util.HashSet<>(entityManager.createQuery(
                "select e.caseId from ReviewCaseEntity e where e.rereviewerHistoryComplete = false", String.class)
                .getResultList());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void pinCaseVersion(String caseId) {
        // 讀入（或沿用本交易已讀入的）案件；之後的強制遞增以這個版本為準。
        caseEntity(caseId);
    }

    @Override
    public boolean hasRead(String caseId, String readerId) {
        return readers.existsByCaseIdAndReaderId(caseId, readerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String caseId, String readerId, Instant readAt) {
        if (readers.existsByCaseIdAndReaderId(caseId, readerId)) return;
        readers.save(new InitialReviewReaderEntity(caseId, readerId, readAt));
        // 只遞增版本，不推進 updatedAt（讀取不是業務更新）；與同時進行的指派或改派只能有一方 commit 成功。
        entityManager.lock(caseEntity(caseId), LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    private ReviewCaseEntity caseEntity(String caseId) {
        ReviewCaseEntity e = entityManager.find(ReviewCaseEntity.class, caseId);
        if (e == null) throw new IllegalStateException("ReviewCase not found while recording an initial reader: " + caseId);
        return e;
    }
}
