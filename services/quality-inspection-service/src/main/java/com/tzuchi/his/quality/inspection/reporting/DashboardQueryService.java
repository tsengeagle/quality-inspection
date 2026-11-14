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

    private String buildWhereClause(DashboardQueryCriteria criteria) {
        StringBuilder sb = new StringBuilder();
        int paramIndex = 1;
        if (criteria.getHospitalId() != null) {
            sb.append("AND e.hospitalId = ?").append(paramIndex);
        }
        if (criteria.getProjectId() != null) {
            sb.append("AND e.projectId = ?").append(paramIndex + (criteria.getHospitalId() != null ? 1 : 0));
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
        return params;
    }
}
