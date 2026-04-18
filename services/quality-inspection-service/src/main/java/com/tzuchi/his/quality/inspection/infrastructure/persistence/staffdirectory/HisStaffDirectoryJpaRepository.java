package com.tzuchi.his.quality.inspection.infrastructure.persistence.staffdirectory;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HisStaffDirectoryJpaRepository extends JpaRepository<HisStaffDirectoryEntity, Long> {
    List<HisStaffDirectoryEntity> findByHospitalIdAndCandidateRole(String hospitalId,
                                                                   AssignmentCandidateRole candidateRole);

    List<HisStaffDirectoryEntity> findByHospitalIdAndStaffId(String hospitalId, String staffId);
}
