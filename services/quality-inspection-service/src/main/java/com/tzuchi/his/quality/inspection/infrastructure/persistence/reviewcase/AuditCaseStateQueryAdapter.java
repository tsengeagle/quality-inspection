package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.application.audit.AuditCaseStateQuery;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** D-071：一次查出一頁稽核紀錄涉及的案件狀態，避免逐案查詢。 */
@Component
public class AuditCaseStateQueryAdapter implements AuditCaseStateQuery {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, CaseState> findStates(Collection<String> caseIds) {
        if (caseIds.isEmpty()) return Map.of();
        return entityManager.createQuery(
                        "select e from ReviewCaseEntity e where e.caseId in :ids", ReviewCaseEntity.class)
                .setParameter("ids", caseIds)
                .getResultList().stream()
                .collect(Collectors.toMap(ReviewCaseEntity::getCaseId,
                        e -> new CaseState(e.getStatus(), e.getRereviewerId())));
    }
}
