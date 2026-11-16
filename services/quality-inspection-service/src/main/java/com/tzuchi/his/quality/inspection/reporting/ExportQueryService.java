package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RereviewerHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExportQueryService {

    private final EntityManager em;
    private final DashboardQueryService dashboardQueryService;
    private final RereviewerHistoryQuery historyQuery;

    /** D-055 第 6 點：進入這些狀態後就不可能再指派或改派複審醫師。 */
    private static final Set<CaseStatus> NO_FURTHER_REREVIEWER = EnumSet.of(
            CaseStatus.REREVIEW_IN_PROGRESS, CaseStatus.CLOSED, CaseStatus.CANCELLED);
    private static final List<String> SCORE_FIELDS = List.of("initialScore", "finalScore");

    public ExportQueryService(EntityManager em, DashboardQueryService dashboardQueryService,
                              RereviewerHistoryQuery historyQuery) {
        this.em = em;
        this.dashboardQueryService = dashboardQueryService;
        this.historyQuery = historyQuery;
    }

    public ExportResult export(ActorContext actor, ExportCriteria criteria) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        if (!"REVIEW_COMPLETION".equals(criteria.getReportType())) {
            throw new BadRequestException("Unsupported reportType: " + criteria.getReportType());
        }

        // D-042、D-047：院區取自登入者，請求帶的院區只有 SYSTEM_ADMIN 能用來縮小範圍
        ExportCriteria scoped = new ExportCriteria(criteria.getReportType(),
                actor.readScopeHospitalId(criteria.getHospitalId()), criteria.getFrom(), criteria.getTo());

        List<ExportRow> rows = maskScores(actor, queryRows(scoped));
        DashboardQueryCriteria dashCriteria = new DashboardQueryCriteria(
                scoped.getHospitalId(), null, scoped.getFrom(), scoped.getTo());
        DashboardSummary summary = dashboardQueryService.getSummary(actor, dashCriteria);

        return new ExportResult(criteria.getReportType(), rows, summary);
    }

    private List<ExportRow> queryRows(ExportCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ReviewCaseEntity> cq = cb.createQuery(ReviewCaseEntity.class);
        Root<ReviewCaseEntity> root = cq.from(ReviewCaseEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        if (criteria.getHospitalId() != null) {
            predicates.add(cb.equal(root.get("hospitalId"), criteria.getHospitalId()));
        }
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(root.get("caseId")));

        TypedQuery<ReviewCaseEntity> q = em.createQuery(cq);
        q.setMaxResults(10000);

        return q.getResultList().stream()
                .map(e -> new ExportRow(
                        e.getCaseId(), e.getProjectId(), e.getHospitalId(),
                        e.getStatus().name(), e.getReviewerId(), e.getRereviewerId(),
                        e.getInitialReviewScore(), e.getFinalScore(),
                        null, e.getClosedAt()))
                .collect(Collectors.toList());
    }

    /**
     * D-055 第 6 點：匯出不記錄讀取者，改為遮罩逐案分數。下列任一成立即遮罩：
     * SYSTEM_ADMIN（metadata／aggregated only）；案件之後仍可能指派複審；匯出者是現任或歷史複審醫師；
     * 該案歷史不完整（無法證明匯出者不是歷史複審醫師）。
     */
    private List<ExportRow> maskScores(ActorContext actor, List<ExportRow> rows) {
        Set<String> historicalCases = historyQuery.findCaseIdsAssignedTo(actor.getUserId());
        Set<String> incompleteCases = historyQuery.findHistoryIncompleteCaseIds();
        return rows.stream()
                .map(r -> mustMask(actor, r, historicalCases, incompleteCases) ? masked(r) : r)
                .collect(Collectors.toList());
    }

    private static boolean mustMask(ActorContext actor, ExportRow r, Set<String> historicalCases,
                                    Set<String> incompleteCases) {
        return actor.hasRole(DomainRole.SYSTEM_ADMIN)
                || !NO_FURTHER_REREVIEWER.contains(CaseStatus.valueOf(r.getStatus()))
                || Objects.equals(actor.getUserId(), r.getRereviewerId())
                || historicalCases.contains(r.getCaseId())
                || incompleteCases.contains(r.getCaseId());
    }

    private static ExportRow masked(ExportRow r) {
        return new ExportRow(r.getCaseId(), r.getProjectId(), r.getHospitalId(), r.getStatus(),
                r.getReviewerId(), r.getRereviewerId(), null, null, r.getCreatedAt(), r.getClosedAt(), SCORE_FIELDS);
    }
}
