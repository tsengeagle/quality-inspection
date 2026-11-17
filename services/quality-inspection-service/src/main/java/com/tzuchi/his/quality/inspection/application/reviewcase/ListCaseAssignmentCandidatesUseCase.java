package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListCaseAssignmentCandidatesUseCase {

    private final ReviewCaseAssignmentQueryPort queryPort;
    private final AssignmentCandidateDirectoryPort candidateDirectoryPort;

    public ListCaseAssignmentCandidatesUseCase(ReviewCaseAssignmentQueryPort queryPort,
                                               AssignmentCandidateDirectoryPort candidateDirectoryPort) {
        this.queryPort = queryPort;
        this.candidateDirectoryPort = candidateDirectoryPort;
    }

    public List<AssignmentCandidateView> execute(ActorContext actor, String caseId,
                                                 AssignmentCandidateRole role,
                                                 String keyword) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        AssignmentCaseSnapshotView snapshot = queryPort.findAssignmentSnapshotById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(snapshot.getHospitalId());

        List<String> allowedActions = resolveAllowedActions(snapshot);

        boolean actionAllowed = isActionAllowed(role, allowedActions);

        return candidateDirectoryPort.findCandidates(snapshot.getHospitalId(), role, keyword).stream()
                .map(entry -> toCandidateView(snapshot, role, actionAllowed, entry))
                .collect(Collectors.toList());
    }

    private boolean isActionAllowed(AssignmentCandidateRole role, List<String> allowedActions) {
        if (role == AssignmentCandidateRole.REVIEWER) {
            return allowedActions.contains("ASSIGN_REVIEWER") || allowedActions.contains("REASSIGN_REVIEWER");
        }
        return allowedActions.contains("ASSIGN_REREVIEWER") || allowedActions.contains("REASSIGN_REREVIEWER");
    }

    private List<String> resolveAllowedActions(AssignmentCaseSnapshotView snapshot) {
        switch (snapshot.getStatus()) {
            case "SELECTED_PENDING_ASSIGNMENT":
                return List.of("ASSIGN_REVIEWER");
            case "PENDING_INITIAL_REVIEW":
            case "RETURNED":
                return snapshot.getReviewerId() == null
                        ? List.of()
                        : List.of("REASSIGN_REVIEWER");
            case "PENDING_REREVIEW":
                return snapshot.getRereviewerId() == null
                        ? List.of("ASSIGN_REREVIEWER")
                        : List.of("REASSIGN_REREVIEWER");
            default:
                return List.of();
        }
    }

    private AssignmentCandidateView toCandidateView(AssignmentCaseSnapshotView snapshot,
                                                    AssignmentCandidateRole role,
                                                    boolean actionAllowed,
                                                    AssignmentCandidateDirectoryEntry entry) {
        String blockedReason = resolveCandidateBlockedReason(snapshot, role, actionAllowed, entry);
        return new AssignmentCandidateView(
                entry.getUserId(),
                entry.getDisplayName(),
                entry.getDisplayLabel(),
                entry.getRole().name(),
                entry.isActive(),
                blockedReason == null,
                blockedReason);
    }

    private String resolveCandidateBlockedReason(AssignmentCaseSnapshotView snapshot,
                                                 AssignmentCandidateRole role,
                                                 boolean actionAllowed,
                                                 AssignmentCandidateDirectoryEntry entry) {
        if (!entry.isActive()) {
            return "CANDIDATE_INACTIVE";
        }
        if (!actionAllowed) {
            return role == AssignmentCandidateRole.REVIEWER
                    ? "INITIAL_ASSIGNMENT_NOT_ALLOWED"
                    : "REREVIEW_ASSIGNMENT_NOT_ALLOWED";
        }
        if (role == AssignmentCandidateRole.REREVIEWER
                && snapshot.getReviewerId() != null
                && snapshot.getReviewerId().equals(entry.getUserId())) {
            return "REREVIEWER_SAME_AS_REVIEWER";
        }
        return null;
    }
}
