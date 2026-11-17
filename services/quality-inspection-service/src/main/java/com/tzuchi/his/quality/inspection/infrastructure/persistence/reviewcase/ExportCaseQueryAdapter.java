package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ExportCaseQuery;
import com.tzuchi.his.quality.inspection.application.common.ExportCaseRecord;
import com.tzuchi.his.quality.inspection.application.common.Page;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** D-057：匯出的逐案查詢，依案件建立時間過濾、依 caseId 遞增分頁。 */
@Component
public class ExportCaseQueryAdapter implements ExportCaseQuery {

    private static final String RANGE = " where e.createdAt >= :from and e.createdAt < :to";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<ExportCaseRecord> findCases(String hospitalId, Instant from, Instant to, int page, int size) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        // hospitalId 為 null 時不加院區條件（不綁定 null 參數，避免各資料庫對 null 型別推斷不同）
        String where = RANGE + (hospitalId == null ? "" : " and e.hospitalId = :hospitalId");

        TypedQuery<Long> count = entityManager.createQuery("select count(e) from ReviewCaseEntity e" + where, Long.class);
        TypedQuery<ReviewCaseEntity> q = entityManager.createQuery(
                "select e from ReviewCaseEntity e" + where + " order by e.caseId asc", ReviewCaseEntity.class);
        for (TypedQuery<?> query : List.of(count, q)) {
            query.setParameter("from", from).setParameter("to", to);
            if (hospitalId != null) query.setParameter("hospitalId", hospitalId);
        }
        long total = count.getSingleResult();
        long offset = (long) page * size;
        if (offset >= total || offset > Integer.MAX_VALUE) {
            return new Page<>(List.of(), page, size, total); // 超出範圍的頁回空內容，不查資料列
        }
        q.setFirstResult((int) offset);
        q.setMaxResults(size);
        List<ExportCaseRecord> content = q.getResultList().stream()
                .map(e -> new ExportCaseRecord(e.getCaseId(), e.getProjectId(), e.getHospitalId(),
                        e.getStatus().name(), e.getReviewerId(), e.getRereviewerId(),
                        e.getInitialReviewScore(), e.getFinalScore(), e.getCreatedAt(), e.getClosedAt()))
                .collect(Collectors.toList());
        return new Page<>(content, page, size, total);
    }
}
