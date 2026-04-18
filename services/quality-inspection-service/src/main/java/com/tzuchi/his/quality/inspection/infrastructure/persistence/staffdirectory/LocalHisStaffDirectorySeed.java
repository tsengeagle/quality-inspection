package com.tzuchi.his.quality.inspection.infrastructure.persistence.staffdirectory;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Local-only seed data so the JPA-backed assignment candidate source stays usable before an upstream HIS sync exists.
 */
@Component
@Profile("local")
public class LocalHisStaffDirectorySeed implements ApplicationRunner {

    private final HisStaffDirectoryJpaRepository repository;

    public LocalHisStaffDirectorySeed(HisStaffDirectoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }

        repository.saveAll(List.of(
                new HisStaffDirectoryEntity("reviewer-1", "Dr. Reviewer One", "H001", AssignmentCandidateRole.REVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-1", "Dr. Reviewer One", "H001", AssignmentCandidateRole.REREVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-2", "Dr. Reviewer Two", "H001", AssignmentCandidateRole.REVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-3", "Dr. Reviewer Three", "H001", AssignmentCandidateRole.REVIEWER, true),
                new HisStaffDirectoryEntity("rereviewer-1", "Dr. ReReviewer One", "H001", AssignmentCandidateRole.REREVIEWER, true),
                new HisStaffDirectoryEntity("rereviewer-2", "Dr. ReReviewer Two", "H001", AssignmentCandidateRole.REREVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-inactive", "Dr. Inactive", "H001", AssignmentCandidateRole.REVIEWER, false)
        ));
    }
}