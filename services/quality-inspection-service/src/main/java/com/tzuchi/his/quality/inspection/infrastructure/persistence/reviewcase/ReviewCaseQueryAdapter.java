package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionEntity;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ReviewCaseQueryAdapter implements ReviewCaseQueryPort {

    private final EntityManager em;

    public ReviewCaseQueryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ReviewCaseEntity> countRoot = countQuery.from(ReviewCaseEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, criteria).toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        // Data
        CriteriaQuery<ReviewCaseEntity> dataQuery = cb.createQuery(ReviewCaseEntity.class);
        Root<ReviewCaseEntity> dataRoot = dataQuery.from(ReviewCaseEntity.class);
        dataQuery.where(buildPredicates(cb, dataRoot, criteria).toArray(new Predicate[0]));
        dataQuery.orderBy(cb.desc(dataRoot.get("caseId")));

        TypedQuery<ReviewCaseEntity> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(criteria.getPage() * criteria.getSize());
        typedQuery.setMaxResults(criteria.getSize());

        List<ReviewCaseSummary> content = typedQuery.getResultList().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        return new Page<>(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public Optional<ReviewCaseDetailView> findDetailById(String caseId) {
        ReviewCaseEntity entity = em.find(ReviewCaseEntity.class, caseId);
        if (entity == null) {
            return Optional.empty();
        }
        List<SubmissionSummaryItem> submissions = loadSubmissions(caseId);
        return Optional.of(toDetailView(entity, submissions));
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<ReviewCaseEntity> root,
                                             ReviewCaseQueryCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.getHospitalId() != null) {
            predicates.add(cb.equal(root.get("hospitalId"), criteria.getHospitalId()));
        }
        if (criteria.getProjectId() != null) {
            predicates.add(cb.equal(root.get("projectId"), criteria.getProjectId()));
        }
        if (criteria.getBatchId() != null) {
            predicates.add(cb.equal(root.get("batchId"), criteria.getBatchId()));
        }
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }
        if (criteria.getReviewerId() != null) {
            predicates.add(cb.equal(root.get("reviewerId"), criteria.getReviewerId()));
        }
        if (criteria.getRereviewerId() != null) {
            predicates.add(cb.equal(root.get("rereviewerId"), criteria.getRereviewerId()));
        }
        if (criteria.getMedicalRecordType() != null) {
            predicates.add(cb.equal(root.get("medicalRecordType"), criteria.getMedicalRecordType()));
        }
        return predicates;
    }

    private List<SubmissionSummaryItem> loadSubmissions(String caseId) {
        TypedQuery<ReviewSubmissionEntity> q = em.createQuery(
                "SELECT s FROM ReviewSubmissionEntity s WHERE s.caseId = :caseId ORDER BY s.revisionNo DESC",
                ReviewSubmissionEntity.class);
        q.setParameter("caseId", caseId);
        return q.getResultList().stream()
                .map(s -> new SubmissionSummaryItem(
                s.getSubmissionId(), s.getStage().name(), s.getReviewerId(),
                s.getRevisionNo(), s.getStatus().name(),
                s.getTotalScore(), s.getSubmittedAt()))
                .collect(Collectors.toList());
    }

    private ReviewCaseSummary toSummary(ReviewCaseEntity e) {
        return new ReviewCaseSummary(
                e.getCaseId(), e.getProjectId(), e.getBatchId(), e.getHospitalId(),
                e.getStatus().name(), e.getReviewerId(), e.getRereviewerId(),
                e.getMedicalRecordType(), e.getSourceCaseKey(),
                null, e.getReviewerAssignedAt(), e.getDueAt(), null);
    }

    private ReviewCaseDetailView toDetailView(ReviewCaseEntity e,
                                               List<SubmissionSummaryItem> submissions) {
        return new ReviewCaseDetailView(
                e.getCaseId(), e.getProjectId(), e.getBatchId(), e.getHospitalId(),
                e.getStatus().name(), e.getReviewerId(), e.getRereviewerId(),
                e.getMedicalRecordType(), e.getSourceCaseKey(),
                e.getInitialReviewScore(), e.getFinalScore(), e.getFinalConclusion(),
                null, e.getReviewerAssignedAt(), e.getDueAt(),
            e.getClosedAt(), e.getCancelledAt(),
            e.getLatestInitialSubmissionId(), e.getScoreSheetVersionRef(), submissions,
            e.getLatestRereviewSubmissionId(), null, null, false, null, null);
    }
}
