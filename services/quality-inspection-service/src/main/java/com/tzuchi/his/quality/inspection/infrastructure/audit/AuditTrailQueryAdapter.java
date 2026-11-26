package com.tzuchi.his.quality.inspection.infrastructure.audit;

import com.tzuchi.his.quality.inspection.application.audit.AuditTrailEntry;
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryCriteria;
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryPort;
import com.tzuchi.his.quality.inspection.application.common.Page;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditTrailQueryAdapter implements AuditTrailQueryPort {

    private final EntityManager em;

    public AuditTrailQueryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<AuditTrailEntry> findAuditTrails(AuditTrailQueryCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AuditTrailEntity> countRoot = countQuery.from(AuditTrailEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, criteria).toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        CriteriaQuery<AuditTrailEntity> dataQuery = cb.createQuery(AuditTrailEntity.class);
        Root<AuditTrailEntity> dataRoot = dataQuery.from(AuditTrailEntity.class);
        dataQuery.where(buildPredicates(cb, dataRoot, criteria).toArray(new Predicate[0]));
        dataQuery.orderBy(cb.desc(dataRoot.get("occurredAt")), cb.desc(dataRoot.get("sequenceNo")));

        TypedQuery<AuditTrailEntity> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(criteria.getPage() * criteria.getSize());
        typedQuery.setMaxResults(criteria.getSize());

        List<AuditTrailEntry> content = typedQuery.getResultList().stream()
                .map(this::toEntry)
                .collect(Collectors.toList());

        return new Page<>(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public List<AuditTrailEntry> findLifecycleByEntityId(String entityType, String entityId) {
        TypedQuery<AuditTrailEntity> q = em.createQuery(
                "SELECT a FROM AuditTrailEntity a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.occurredAt ASC, a.sequenceNo ASC",
                AuditTrailEntity.class);
        q.setParameter("entityType", entityType);
        q.setParameter("entityId", entityId);
        return q.getResultList().stream()
                .map(this::toEntry)
                .collect(Collectors.toList());
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<AuditTrailEntity> root,
                                             AuditTrailQueryCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.getAggregateType() != null) {
            predicates.add(cb.equal(root.get("entityType"), criteria.getAggregateType()));
        }
        if (criteria.getAggregateId() != null) {
            predicates.add(cb.equal(root.get("entityId"), criteria.getAggregateId()));
        }
        if (criteria.getHospitalId() != null) {
            predicates.add(cb.equal(root.get("hospitalId"), criteria.getHospitalId()));
        }
        if (criteria.getEventName() != null) {
            predicates.add(cb.equal(root.get("action"), criteria.getEventName()));
        }
        if (criteria.getFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), criteria.getFrom()));
        }
        if (criteria.getTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), criteria.getTo()));
        }
        return predicates;
    }

    private AuditTrailEntry toEntry(AuditTrailEntity e) {
        return new AuditTrailEntry(
                e.getId(), e.getEntityType(), e.getEntityId(), e.getAction(),
                e.getActorUserId(), e.getActorUserName(), e.getHospitalId(),
                e.getOccurredAt(), e.getDetail());
    }
}
