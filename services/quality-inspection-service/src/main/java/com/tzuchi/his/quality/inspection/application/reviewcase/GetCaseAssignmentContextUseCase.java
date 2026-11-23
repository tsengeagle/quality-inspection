package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetCaseAssignmentContextUseCase {

    private final ReviewCaseAssignmentQueryPort queryPort;
    private final AssignmentCandidateDirectoryPort candidateDirectoryPort;

    public GetCaseAssignmentContextUseCase(ReviewCaseAssignmentQueryPort queryPort,
                                           AssignmentCandidateDirectoryPort candidateDirectoryPort) {
        this.queryPort = queryPort;
        this.candidateDirectoryPort = candidateDirectoryPort;
    }

    public AssignmentBootstrapView execute(ActorContext actor, String caseId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        AssignmentCaseSnapshotView snapshot = queryPort.findAssignmentSnapshotById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(snapshot.getHospitalId());

        String pageMode = resolvePageMode(snapshot);
        String blockedReason = resolveBlockedReason(snapshot);
        boolean blocked = blockedReason != null;
        List<String> allowedActions = blocked ? Collections.emptyList() : resolveAllowedActions(snapshot);

        return new AssignmentBootstrapView(
                snapshot.getCaseId(),
                snapshot.getProjectId(),
            snapshot.getProjectName(),
                snapshot.getBatchId(),
                snapshot.getHospitalId(),
            snapshot.getSourceCaseKey(),
                snapshot.getMedicalRecordType(),
                snapshot.getStatus(),
                pageMode,
                blocked,
                blockedReason,
                allowedActions,
                resolveAssigneeSummary(snapshot.getHospitalId(), snapshot.getReviewerId(), snapshot.getReviewerAssignedAt()),
                resolveAssigneeSummary(snapshot.getHospitalId(), snapshot.getRereviewerId(), snapshot.getRereviewerAssignedAt()),
                snapshot.getReviewerDueAt(),
                snapshot.getRereviewerDueAt(),
                resolveEffectiveDueAt(snapshot));
    }

    private AssignmentAssigneeSummaryView resolveAssigneeSummary(String hospitalId,
                                                                 String userId,
                                                                 Instant assignedAt) {
        if (userId == null) {
            return null;
        }
        Optional<AssignmentCandidateDirectoryEntry> resolved = candidateDirectoryPort.findByUserId(hospitalId, userId);
        return resolved
                .map(entry -> new AssignmentAssigneeSummaryView(
                        entry.getUserId(),
                        entry.getDisplayName(),
                        entry.getDisplayLabel(),
                        assignedAt))
                .orElseGet(() -> new AssignmentAssigneeSummaryView(userId, userId, userId, assignedAt));
    }

    private String resolvePageMode(AssignmentCaseSnapshotView snapshot) {
        if (resolveBlockedReason(snapshot) != null) {
            return "READONLY_BLOCKED";
        }
        if ("PENDING_REREVIEW".equals(snapshot.getStatus())) {
            return "MIXED_ASSIGNMENT";
        }
        if (snapshot.getReviewerId() != null) {
            return "REASSIGN_INITIAL";
        }
        return "ASSIGN_INITIAL";
    }

    private List<String> resolveAllowedActions(AssignmentCaseSnapshotView snapshot) {
        switch (snapshot.getStatus()) {
            case "SELECTED_PENDING_ASSIGNMENT":
                return List.of("ASSIGN_REVIEWER");
            case "PENDING_INITIAL_REVIEW":
            case "RETURNED":
                return snapshot.getReviewerId() == null
                        ? Collections.emptyList()
                        : List.of("REASSIGN_REVIEWER");
            case "PENDING_REREVIEW":
                return snapshot.getRereviewerId() == null
                        ? List.of("ASSIGN_REREVIEWER")
                        : List.of("REASSIGN_REREVIEWER");
            default:
                return Collections.emptyList();
        }
    }

    private String resolveBlockedReason(AssignmentCaseSnapshotView snapshot) {
        switch (snapshot.getStatus()) {
            case "CLOSED":
                return "CASE_ALREADY_CLOSED";
            case "CANCELLED":
                return "CASE_ALREADY_CANCELLED";
            case "INITIAL_REVIEW_IN_PROGRESS":
            case "INITIAL_REVIEW_COMPLETED":
            case "REREVIEW_IN_PROGRESS":
                return "CASE_NOT_ASSIGNABLE_IN_CURRENT_STATUS";
            case "PENDING_INITIAL_REVIEW":
            case "RETURNED":
                return snapshot.getReviewerId() == null
                        ? "REVIEWER_ASSIGNMENT_MISSING"
                        : null;
            case "SELECTED_PENDING_ASSIGNMENT":
            case "PENDING_REREVIEW":
                return null;
            default:
                return "CASE_NOT_ASSIGNABLE_IN_CURRENT_STATUS";
        }
    }

    private Instant resolveEffectiveDueAt(AssignmentCaseSnapshotView snapshot) {
        if ("PENDING_REREVIEW".equals(snapshot.getStatus())) {
            return snapshot.getRereviewerDueAt();
        }
        return snapshot.getReviewerDueAt();
    }
}
