package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final EntityManager em;

    public DashboardQueryService(EntityManager em) {
        this.em = em;
    }

    public DashboardSummary getSummary(ActorContext actor, DashboardQueryCriteria criteria) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        // D-042、D-047：院區取自登入者，請求帶的院區只有 SYSTEM_ADMIN 能用來縮小範圍
        DashboardQueryCriteria scoped = new DashboardQueryCriteria(
                actor.readScopeHospitalId(criteria.getHospitalId()),
                criteria.getProjectId(), criteria.getFrom(), criteria.getTo());

        String whereClause = buildWhereClause(scoped);
        List<Object> params = buildParams(scoped);

        long total = countWithCondition("", whereClause, params);
        long closed = countWithCondition("AND e.status = 'CLOSED'", whereClause, params);
        long rereviewed = countWithCondition("AND e.rereviewerId IS NOT NULL", whereClause, params);
        long overdue = countWithCondition(
                "AND e.dueAt < CURRENT_TIMESTAMP AND e.status NOT IN ('CLOSED', 'CANCELLED')",
                whereClause, params);

        return new DashboardSummary(total, closed, rereviewed, overdue);
    }

    private long countWithCondition(String extraCondition, String whereClause,
                                     List<Object> params) {
        String jpql = "SELECT COUNT(e) FROM ReviewCaseEntity e WHERE 1=1 "
                + whereClause + " " + extraCondition;
        Query q = em.createQuery(jpql);
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }
        return ((Number) q.getSingleResult()).longValue();
    }

    /**
     * 院區、專案，以及 D-057 第 4 點的時間區間：from、to 兩個都有值時，只統計
     * from ≤ createdAt < to 的案件（建立時間為 NULL 的不計入）；兩個都沒有時不依時間過濾。
     * 只給其中一個或 from 不早於 to 的請求由 controller 回 400，不會到這裡。
     */
    private String buildWhereClause(DashboardQueryCriteria criteria) {
        StringBuilder sb = new StringBuilder();
        int paramIndex = 1;
        if (criteria.getHospitalId() != null) {
            sb.append("AND e.hospitalId = ?").append(paramIndex++).append(' ');
        }
        if (criteria.getProjectId() != null) {
            sb.append("AND e.projectId = ?").append(paramIndex++).append(' ');
        }
        if (hasRange(criteria)) {
            sb.append("AND e.createdAt >= ?").append(paramIndex++).append(' ');
            sb.append("AND e.createdAt < ?").append(paramIndex++).append(' ');
        }
        return sb.toString();
    }

    private List<Object> buildParams(DashboardQueryCriteria criteria) {
        List<Object> params = new ArrayList<>();
        if (criteria.getHospitalId() != null) {
            params.add(criteria.getHospitalId());
        }
        if (criteria.getProjectId() != null) {
            params.add(criteria.getProjectId());
        }
        if (hasRange(criteria)) {
            params.add(criteria.getFrom());
            params.add(criteria.getTo());
        }
        return params;
    }

    private static boolean hasRange(DashboardQueryCriteria criteria) {
        return criteria.getFrom() != null && criteria.getTo() != null;
    }
}
