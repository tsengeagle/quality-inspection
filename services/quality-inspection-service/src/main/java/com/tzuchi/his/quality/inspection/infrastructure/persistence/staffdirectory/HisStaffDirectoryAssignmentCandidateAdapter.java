package com.tzuchi.his.quality.inspection.infrastructure.persistence.staffdirectory;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateDirectoryEntry;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateDirectoryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateRole;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Minimal first-version assignment candidate source adapter backed by a persisted HIS staff directory snapshot.
 * Business eligibility remains in the application layer; this adapter only resolves stable directory fields.
 */
@Component
public class HisStaffDirectoryAssignmentCandidateAdapter implements AssignmentCandidateDirectoryPort {

    private final HisStaffDirectoryJpaRepository repository;

    public HisStaffDirectoryAssignmentCandidateAdapter(HisStaffDirectoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AssignmentCandidateDirectoryEntry> findCandidates(String hospitalId,
                                                                  AssignmentCandidateRole role,
                                                                  String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return repository.findByHospitalIdAndCandidateRole(hospitalId, role).stream()
                .filter(entry -> normalizedKeyword.isEmpty() || containsKeyword(entry, normalizedKeyword))
                .sorted(Comparator.comparing(HisStaffDirectoryEntity::getStaffId))
                .map(this::toDirectoryEntry)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AssignmentCandidateDirectoryEntry> findByUserId(String hospitalId, String userId) {
        return repository.findByHospitalIdAndStaffId(hospitalId, userId).stream()
                .sorted(Comparator.comparing(HisStaffDirectoryEntity::isActive).reversed()
                        .thenComparing(entity -> entity.getCandidateRole().name()))
                .findFirst()
                .map(this::toDirectoryEntry);
    }

    private AssignmentCandidateDirectoryEntry toDirectoryEntry(HisStaffDirectoryEntity entity) {
        return new AssignmentCandidateDirectoryEntry(
                entity.getStaffId(),
                entity.getDisplayName(),
                entity.getStaffId() + " | " + entity.getDisplayName(),
                entity.getCandidateRole(),
                entity.isActive());
    }

    private boolean containsKeyword(HisStaffDirectoryEntity entry, String keyword) {
        return entry.getStaffId().toLowerCase(Locale.ROOT).contains(keyword)
                || entry.getDisplayName().toLowerCase(Locale.ROOT).contains(keyword)
                || (entry.getStaffId() + " | " + entry.getDisplayName()).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }
}