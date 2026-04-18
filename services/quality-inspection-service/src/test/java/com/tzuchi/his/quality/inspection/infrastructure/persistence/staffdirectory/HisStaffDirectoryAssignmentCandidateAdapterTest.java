package com.tzuchi.his.quality.inspection.infrastructure.persistence.staffdirectory;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateDirectoryEntry;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(HisStaffDirectoryAssignmentCandidateAdapter.class)
class HisStaffDirectoryAssignmentCandidateAdapterTest {

    @Autowired
    private HisStaffDirectoryJpaRepository repository;

    @Autowired
    private HisStaffDirectoryAssignmentCandidateAdapter adapter;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.saveAll(List.of(
                new HisStaffDirectoryEntity("reviewer-1", "Dr. Reviewer One", "H001", AssignmentCandidateRole.REVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-1", "Dr. Reviewer One", "H001", AssignmentCandidateRole.REREVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-2", "Dr. Reviewer Two", "H001", AssignmentCandidateRole.REVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-9", "Dr. External", "H002", AssignmentCandidateRole.REVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-inactive", "Dr. Inactive", "H001", AssignmentCandidateRole.REVIEWER, false)
        ));
    }

    @Test
    void findCandidates_filtersByHospitalRoleAndKeyword() {
        List<AssignmentCandidateDirectoryEntry> result = adapter.findCandidates("H001",
                AssignmentCandidateRole.REVIEWER, "Two");

        assertEquals(1, result.size());
        assertEquals("reviewer-2", result.get(0).getUserId());
        assertEquals("reviewer-2 | Dr. Reviewer Two", result.get(0).getDisplayLabel());
        assertEquals(AssignmentCandidateRole.REVIEWER, result.get(0).getRole());
    }

    @Test
    void findByUserId_prefersActiveEntryWithinHospitalScope() {
        Optional<AssignmentCandidateDirectoryEntry> result = adapter.findByUserId("H001", "reviewer-1");

        assertTrue(result.isPresent());
        assertEquals("reviewer-1", result.get().getUserId());
        assertEquals("Dr. Reviewer One", result.get().getDisplayName());
        assertTrue(result.get().isActive());
    }
}
