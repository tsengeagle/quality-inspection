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
    private final InitialReviewReaderPort readerPort;

    public ListCaseAssignmentCandidatesUseCase(ReviewCaseAssignmentQueryPort queryPort,
                                               AssignmentCandidateDirectoryPort candidateDirectoryPort,
                                               InitialReviewReaderPort readerPort) {
        this.queryPort = queryPort;
        this.candidateDirectoryPort = candidateDirectoryPort;
        this.readerPort = readerPort;
    }

    public List<AssignmentCandidateView> execute(ActorContext actor, String caseId,
                                                 AssignmentCandidateRole role,
                                                 String keyword) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        AssignmentCaseSnapshotView snapshot = queryPort.findAssignmentSnapshotById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(snapshot.getHospitalId());

        // D-084 第 5 點：與分派頁共用同一份可執行動作的判斷
        List<String> allowedActions = GetCaseAssignmentContextUseCase.allowedActions(snapshot);

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
        // D-084 第 6 點：與指派的 422 一致（D-055 第 5 點）；只讀紀錄，不寫入
        if (role == AssignmentCandidateRole.REREVIEWER
                && readerPort.hasRead(snapshot.getCaseId(), entry.getUserId())) {
            return "REREVIEWER_HAS_READ_INITIAL";
        }
        return null;
    }
}
