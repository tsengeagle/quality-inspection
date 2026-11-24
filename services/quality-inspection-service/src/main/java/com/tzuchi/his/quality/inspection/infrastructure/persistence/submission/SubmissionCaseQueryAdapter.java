package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.application.audit.SubmissionCaseQuery;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** D-078 第 2 點：一次查出一頁稽核紀錄涉及的提交所屬案件，避免逐筆查詢。 */
@Component
public class SubmissionCaseQueryAdapter implements SubmissionCaseQuery {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, String> findCaseIds(Collection<String> submissionIds) {
        if (submissionIds.isEmpty()) return Map.of();
        return entityManager.createQuery(
                        "select e from ReviewSubmissionEntity e where e.submissionId in :ids", ReviewSubmissionEntity.class)
                .setParameter("ids", submissionIds)
                .getResultList().stream()
                .collect(Collectors.toMap(ReviewSubmissionEntity::getSubmissionId, ReviewSubmissionEntity::getCaseId));
    }
}
