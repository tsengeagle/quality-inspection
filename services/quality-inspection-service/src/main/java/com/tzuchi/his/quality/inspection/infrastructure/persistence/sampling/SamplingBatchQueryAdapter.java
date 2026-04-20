package com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling;

import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.sampling.*;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectEntity;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SamplingBatchQueryAdapter implements SamplingBatchQueryPort {

    private final EntityManager em;

    public SamplingBatchQueryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<SamplingBatchSummary> findBatches(SamplingBatchQueryCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SamplingBatchEntity> countRoot = countQuery.from(SamplingBatchEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, criteria).toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<SamplingBatchEntity> dataQuery = cb.createQuery(SamplingBatchEntity.class);
        Root<SamplingBatchEntity> dataRoot = dataQuery.from(SamplingBatchEntity.class);
        dataQuery.where(buildPredicates(cb, dataRoot, criteria).toArray(new Predicate[0]));
        dataQuery.orderBy(cb.desc(dataRoot.get("startedAt")));

        TypedQuery<SamplingBatchEntity> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(criteria.getPage() * criteria.getSize());
        typedQuery.setMaxResults(criteria.getSize());

        List<SamplingBatchSummary> content = typedQuery.getResultList().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        return new Page<>(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public Optional<SamplingBatchDetailView> findDetailById(String batchId) {
        SamplingBatchEntity entity = em.find(SamplingBatchEntity.class, batchId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDetailView(entity));
    }

    @Override
    public Optional<SamplingBatchDetailView> findLatestDetailByProjectId(String projectId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SamplingBatchEntity> query = cb.createQuery(SamplingBatchEntity.class);
        Root<SamplingBatchEntity> root = query.from(SamplingBatchEntity.class);

        query.where(cb.equal(root.get("projectId"), projectId));
        query.orderBy(cb.desc(root.get("startedAt")));

        List<SamplingBatchEntity> results = em.createQuery(query)
                .setMaxResults(1)
                .getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(toDetailView(results.get(0)));
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<SamplingBatchEntity> root,
                                             SamplingBatchQueryCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.getProjectId() != null) {
            predicates.add(cb.equal(root.get("projectId"), criteria.getProjectId()));
        }
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }
        if (criteria.getExecutionMode() != null) {
            predicates.add(cb.equal(root.get("executionMode"), criteria.getExecutionMode()));
        }
        return predicates;
    }

    private SamplingBatchSummary toSummary(SamplingBatchEntity e) {
        ReviewProjectEntity project = em.find(ReviewProjectEntity.class, e.getProjectId());
        return new SamplingBatchSummary(
                e.getBatchId(),
                e.getProjectId(),
            project != null ? project.getName() : e.getProjectId(),
                e.getExecutionMode(),
                e.getStatus(),
                e.getPopulationCount(),
                e.getEligibleCount(),
            e.getExcludedCount(),
                e.getSelectedCount(),
            e.getProjectVersionId(),
            e.getProjectVersionNumber(),
                e.getRetryOfBatchId(),
                e.getStartedAt(),
                e.getFinishedAt()
        );
    }

    private SamplingBatchDetailView toDetailView(SamplingBatchEntity e) {
        RuleSnapshotSummary snapshotSummary = new RuleSnapshotSummary(
                e.getProjectVersionId(),
                e.getProjectVersionNumber(),
                e.getScoreSheetVersionId(),
                e.getSamplingPolicy()
        );

        List<SelectedCaseRefView> cases = e.getSelectedCases().stream()
                .map(c -> new SelectedCaseRefView(
                        c.getSourceCaseKey(),
                        c.getHospitalId(),
                        c.getMedicalRecordType()))
                .collect(Collectors.toList());

        return new SamplingBatchDetailView(
                e.getBatchId(),
                e.getProjectId(),
                e.getExecutionMode(),
                e.getStatus(),
                e.getPopulationCount(),
                e.getEligibleCount(),
                e.getExcludedCount(),
                e.getSelectedCount(),
                e.getRetryOfBatchId(),
                e.getFailureReason(),
                e.getStartedAt(),
                e.getFinishedAt(),
                snapshotSummary,
                cases
        );
    }
}
