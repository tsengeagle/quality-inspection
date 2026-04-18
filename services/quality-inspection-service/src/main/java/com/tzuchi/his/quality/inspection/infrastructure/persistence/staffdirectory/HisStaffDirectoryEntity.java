package com.tzuchi.his.quality.inspection.infrastructure.persistence.staffdirectory;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateRole;

import javax.persistence.*;

@Entity
@Table(name = "his_staff_directory",
        uniqueConstraints = @UniqueConstraint(name = "uk_his_staff_directory_scope_role",
                columnNames = {"hospital_id", "staff_id", "candidate_role"}))
public class HisStaffDirectoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "directory_entry_id")
    private Long directoryEntryId;

    @Column(name = "staff_id", nullable = false)
    private String staffId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_role", nullable = false)
    private AssignmentCandidateRole candidateRole;

    @Column(name = "active", nullable = false)
    private boolean active;

    public HisStaffDirectoryEntity() {
    }

    public HisStaffDirectoryEntity(String staffId,
                                   String displayName,
                                   String hospitalId,
                                   AssignmentCandidateRole candidateRole,
                                   boolean active) {
        this.staffId = staffId;
        this.displayName = displayName;
        this.hospitalId = hospitalId;
        this.candidateRole = candidateRole;
        this.active = active;
    }

    public Long getDirectoryEntryId() {
        return directoryEntryId;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public AssignmentCandidateRole getCandidateRole() {
        return candidateRole;
    }

    public void setCandidateRole(AssignmentCandidateRole candidateRole) {
        this.candidateRole = candidateRole;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
