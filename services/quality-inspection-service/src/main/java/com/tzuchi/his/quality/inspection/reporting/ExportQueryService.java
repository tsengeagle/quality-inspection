package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExportQueryService {

    private final EntityManager em;
    private final DashboardQueryService dashboardQueryService;

    public ExportQueryService(EntityManager em, DashboardQueryService dashboardQueryService) {
        this.em = em;
        this.dashboardQueryService = dashboardQueryService;
    }

    public ExportResult export(ActorContext actor, ExportCriteria criteria) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        if (!"REVIEW_COMPLETION".equals(criteria.getReportType())) {
            throw new BadRequestException("Unsupported reportType: " + criteria.getReportType());
        }

        // D-042、D-047：院區取自登入者，請求帶的院區只有 SYSTEM_ADMIN 能用來縮小範圍
        ExportCriteria scoped = new ExportCriteria(criteria.getReportType(),
                actor.readScopeHospitalId(criteria.getHospitalId()), criteria.getFrom(), criteria.getTo());

        List<ExportRow> rows = queryRows(scoped);
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
}
