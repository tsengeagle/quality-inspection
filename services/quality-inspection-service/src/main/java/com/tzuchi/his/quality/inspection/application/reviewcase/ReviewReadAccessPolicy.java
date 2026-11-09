package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.*;
import org.springframework.stereotype.Component;
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryPort;
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

import java.util.List;
import java.util.Objects;

@Component
public class ReviewReadAccessPolicy {
    private final AuditTrailQueryPort auditQueryPort;
    private final ObjectMapper objectMapper;

    public ReviewReadAccessPolicy(AuditTrailQueryPort auditQueryPort, ObjectMapper objectMapper) {
        this.auditQueryPort = auditQueryPort;
        this.objectMapper = objectMapper;
    }

    public void requireAccess(ActorContext actor, ReviewCaseDetailView view) {
        requireAccess(actor, view.getCaseId(), view.getHospitalId(), view.getReviewerId(), view.getRereviewerId(),
                view.getSubmissions().stream().anyMatch(s -> "INITIAL".equals(s.getStage())
                        && submitted(s.getStatus()) && Objects.equals(actor.getUserId(), s.getReviewerId())),
                view.getSubmissions().stream().anyMatch(s -> "REREVIEW".equals(s.getStage())
                        && submitted(s.getStatus()) && Objects.equals(actor.getUserId(), s.getReviewerId())));
    }

    public void requireAccess(ActorContext actor, ReviewCase rc, List<ReviewSubmission> submissions) {
        requireAccess(actor, rc.getCaseId(), rc.getHospitalId(),
                rc.getReviewerAssignment() == null ? null : rc.getReviewerAssignment().getReviewerId(),
                rc.getRereviewerAssignment() == null ? null : rc.getRereviewerAssignment().getReviewerId(),
                submissions.stream().anyMatch(s -> s.getStage() == ReviewStage.INITIAL
                        && submitted(s.getStatus().name()) && Objects.equals(actor.getUserId(), s.getReviewerId())),
                submissions.stream().anyMatch(s -> s.getStage() == ReviewStage.REREVIEW
                        && submitted(s.getStatus().name()) && Objects.equals(actor.getUserId(), s.getReviewerId())));
    }

    private void requireAccess(ActorContext actor, String caseId, String hospital, String reviewer, String rereviewer,
                               boolean initialSubmitter, boolean rereviewSubmitter) {
        if (actor.getRoles().size() != 1) {
            throw new AccessDeniedException("Multi-role read authorization is pending confirmation");
        }
        actor.requireHospitalScope(hospital);
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) return;
        boolean allowed = actor.hasRole(DomainRole.HOSPITAL_MANAGER) || actor.hasRole(DomainRole.AUDITOR);
        allowed |= actor.hasRole(DomainRole.REVIEWER)
                && (Objects.equals(actor.getUserId(), reviewer) || initialSubmitter);
        allowed |= actor.hasRole(DomainRole.REREVIEWER)
                && (Objects.equals(actor.getUserId(), rereviewer) || rereviewSubmitter);
        if (!allowed) throw new AccessDeniedException("Case is outside the actor's assignment/submission scope");
        // This role only receives its existing blind/own-rereview projection, never full initial content.
        if (actor.hasRole(DomainRole.REREVIEWER)) return;
        HistoricalIdentity identity = Objects.equals(actor.getUserId(), rereviewer) || rereviewSubmitter
                ? HistoricalIdentity.PROVEN_HISTORICAL
                : assessHistoricalIdentity(caseId, hospital, actor.getUserId());
        if (identity != HistoricalIdentity.PROVEN_NON_HISTORICAL) {
            throw new AccessDeniedException("Historical rereviewer identity cannot be reliably excluded");
        }
    }

    enum HistoricalIdentity {
        PROVEN_HISTORICAL, PROVEN_NON_HISTORICAL, UNKNOWN
    }

    HistoricalIdentity assessHistoricalIdentity(String caseId, String hospital, String doctorId) {
        for (AuditTrailEntry entry : auditQueryPort.findLifecycleByEntityId("ReviewCase", caseId)) {
            if (!"ReviewCase".equals(entry.getEntityType()) || !caseId.equals(entry.getEntityId())
                    || !hospital.equals(entry.getHospitalId()) || !"REREVIEWER_ASSIGNED".equals(entry.getAction())) {
                continue;
            }
            try {
                JsonNode detail = entry.getDetail() == null ? null : objectMapper.readTree(entry.getDetail());
                JsonNode assignee = detail == null ? null : detail.get("rereviewerId");
                if (assignee == null || !assignee.isTextual() || assignee.asText().isBlank()) {
                    return HistoricalIdentity.UNKNOWN;
                }
                if (doctorId.equals(assignee.asText())) return HistoricalIdentity.PROVEN_HISTORICAL;
            } catch (IOException ex) {
                return HistoricalIdentity.UNKNOWN;
            }
        }
        // No current evidence source certifies complete negative history, including an empty result.
        return HistoricalIdentity.UNKNOWN;
    }

    public boolean canReadContent(ActorContext actor, String stage, String submitter) {
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) return false;
        if (actor.hasRole(DomainRole.REREVIEWER)) {
            return "REREVIEW".equals(stage) && Objects.equals(actor.getUserId(), submitter);
        }
        return true;
    }

    private boolean submitted(String status) {
        return "SUBMITTED".equals(status) || "SUPERSEDED".equals(status);
    }
}
