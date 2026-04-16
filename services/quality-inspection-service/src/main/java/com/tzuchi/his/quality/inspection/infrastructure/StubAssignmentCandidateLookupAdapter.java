package com.tzuchi.his.quality.inspection.infrastructure;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Stub implementation of AssignmentCandidateDirectoryPort for local development.
 * This defines the formal UI-011 candidate lookup contract but is not a production source.
 */
@Component
public class StubAssignmentCandidateLookupAdapter implements AssignmentCandidateDirectoryPort {

    private static final List<StubCandidate> CANDIDATES = List.of(
            new StubCandidate("reviewer-1", "Dr. Reviewer One", "reviewer-1 | Dr. Reviewer One", "H001",
                    List.of(AssignmentCandidateRole.REVIEWER, AssignmentCandidateRole.REREVIEWER), true),
            new StubCandidate("reviewer-2", "Dr. Reviewer Two", "reviewer-2 | Dr. Reviewer Two", "H001",
                    List.of(AssignmentCandidateRole.REVIEWER), true),
            new StubCandidate("reviewer-3", "Dr. Reviewer Three", "reviewer-3 | Dr. Reviewer Three", "H001",
                    List.of(AssignmentCandidateRole.REVIEWER), true),
            new StubCandidate("rereviewer-1", "Dr. ReReviewer One", "rereviewer-1 | Dr. ReReviewer One", "H001",
                    List.of(AssignmentCandidateRole.REREVIEWER), true),
            new StubCandidate("rereviewer-2", "Dr. ReReviewer Two", "rereviewer-2 | Dr. ReReviewer Two", "H001",
                    List.of(AssignmentCandidateRole.REREVIEWER), true),
            new StubCandidate("reviewer-inactive", "Dr. Inactive", "reviewer-inactive | Dr. Inactive", "H001",
                    List.of(AssignmentCandidateRole.REVIEWER), false)
    );

    @Override
    public List<AssignmentCandidateDirectoryEntry> findCandidates(String hospitalId,
                                                                  AssignmentCandidateRole role,
                                                                  String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return CANDIDATES.stream()
                .filter(candidate -> candidate.hospitalId.equals(hospitalId))
                .filter(candidate -> candidate.roles.contains(role))
                .filter(candidate -> normalizedKeyword.isEmpty() || containsKeyword(candidate, normalizedKeyword))
                .sorted(Comparator.comparing(candidate -> candidate.userId))
                .map(candidate -> new AssignmentCandidateDirectoryEntry(
                        candidate.userId,
                        candidate.displayName,
                        candidate.displayLabel,
                        role,
                        candidate.active))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AssignmentCandidateDirectoryEntry> findByUserId(String hospitalId, String userId) {
        return CANDIDATES.stream()
                .filter(candidate -> candidate.hospitalId.equals(hospitalId))
                .filter(candidate -> candidate.userId.equals(userId))
                .findFirst()
                .map(candidate -> new AssignmentCandidateDirectoryEntry(
                        candidate.userId,
                        candidate.displayName,
                        candidate.displayLabel,
                        candidate.roles.isEmpty() ? AssignmentCandidateRole.REVIEWER : candidate.roles.get(0),
                        candidate.active));
    }

    private boolean containsKeyword(StubCandidate candidate, String keyword) {
        List<String> haystack = new ArrayList<>();
        haystack.add(candidate.userId.toLowerCase(Locale.ROOT));
        haystack.add(candidate.displayName.toLowerCase(Locale.ROOT));
        haystack.add(candidate.displayLabel.toLowerCase(Locale.ROOT));
        return haystack.stream().anyMatch(value -> value.contains(keyword));
    }

    private static final class StubCandidate {
        private final String userId;
        private final String displayName;
        private final String displayLabel;
        private final String hospitalId;
        private final List<AssignmentCandidateRole> roles;
        private final boolean active;

        private StubCandidate(String userId, String displayName, String displayLabel,
                              String hospitalId, List<AssignmentCandidateRole> roles,
                              boolean active) {
            this.userId = userId;
            this.displayName = displayName;
            this.displayLabel = displayLabel;
            this.hospitalId = hospitalId;
            this.roles = roles;
            this.active = active;
        }
    }
}
