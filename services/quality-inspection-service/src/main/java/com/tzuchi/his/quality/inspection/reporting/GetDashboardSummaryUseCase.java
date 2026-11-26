package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 儀表板摘要（specs/review-case 的 GetDashboardSummary，D-087）。
 */
@Service
@Transactional(readOnly = true)
public class GetDashboardSummaryUseCase {

    private final EntityManager em;
    private final Clock clock;

    public GetDashboardSummaryUseCase(EntityManager em, Clock clock) {
        this.clock = clock;
        this.em = em;
    }

    /**
     * 檢查的順序（D-087 第 2 點）：角色 → from、to。
     * RC-DSH-01：from、to 只給一個；RC-DSH-02：from 不早於 to，都是 BAD_REQUEST。
     */
    public DashboardSummary execute(ActorContext actor, String hospitalId, String projectId,
                                    Instant from, Instant to) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        if ((from == null) != (to == null)) {
            throw new DomainValidationException("BAD_REQUEST", "from and to must be given together");
        }
        if (from != null && !from.isBefore(to)) {
            throw new DomainValidationException("BAD_REQUEST", "from must be earlier than to");
        }
        // D-042、D-047：院區取自登入者，請求帶的院區只有 SYSTEM_ADMIN 能用來縮小範圍
        return summarize(actor.readScopeHospitalId(hospitalId), projectId, from, to);
    }

    /**
     * 已決定範圍之後的統計，給匯出的 summary 共用（RC-RPT-12）。不檢查角色與輸入。
     * from、to 兩個都有值時，只統計 from ≤ createdAt < to 的案件（建立時間為 NULL 的不計入）。
     */
    DashboardSummary summarize(String hospitalId, String projectId, Instant from, Instant to) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (hospitalId != null) {
            params.add(hospitalId);
            where.append("AND e.hospitalId = ?").append(params.size()).append(' ');
        }
        if (projectId != null) {
            params.add(projectId);
            where.append("AND e.projectId = ?").append(params.size()).append(' ');
        }
        if (from != null && to != null) {
            params.add(from);
            where.append("AND e.createdAt >= ?").append(params.size()).append(' ');
            params.add(to);
            where.append("AND e.createdAt < ?").append(params.size()).append(' ');
        }

        // 4 個數字以單一查詢一次算出：單一 SQL 敘述讀同一個時間點（Oracle READ COMMITTED 的敘述層級一致讀取），
        // 查詢之間提交的案件不會只算進其中幾個數字，比率的分子分母也來自同一個母體。
        // RC-DSH-07（D-087 第 4 點）：已取消的案件不計入任何件數。
        // D-092 第 5 點：逾期的「現在」取注入的時鐘，不取資料庫的時間。逾期的規則仍未立法（D-087 第 5 點）。
        params.add(Instant.now(clock));
        int nowIndex = params.size();
        String jpql = "SELECT COUNT(e),"
                + " COALESCE(SUM(CASE WHEN e.status = 'CLOSED' THEN 1 ELSE 0 END), 0),"
                + " COALESCE(SUM(CASE WHEN e.rereviewerId IS NOT NULL THEN 1 ELSE 0 END), 0),"
                + " COALESCE(SUM(CASE WHEN e.dueAt < ?" + nowIndex
                + " AND e.status <> 'CLOSED' THEN 1 ELSE 0 END), 0)"
                + " FROM ReviewCaseEntity e WHERE e.status <> 'CANCELLED' " + where;

        Query q = em.createQuery(jpql);
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }
        Object[] row = (Object[]) q.getSingleResult();
        return new DashboardSummary(count(row[0]), count(row[1]), count(row[2]), count(row[3]));
    }

    private static long count(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
