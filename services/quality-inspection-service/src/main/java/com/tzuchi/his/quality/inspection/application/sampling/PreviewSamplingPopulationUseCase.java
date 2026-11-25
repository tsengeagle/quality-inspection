package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.SelectedCaseRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Preview sampling population without creating a batch.
 * Per domain model: preview does NOT create a SamplingBatch.
 */
@Service
public class PreviewSamplingPopulationUseCase {

    private final ReviewProjectRepository projectRepository;
    private final CandidateSourcePort candidateSource;

    public PreviewSamplingPopulationUseCase(ReviewProjectRepository projectRepository,
                                             CandidateSourcePort candidateSource) {
        this.projectRepository = projectRepository;
        this.candidateSource = candidateSource;
    }

    public SamplingPreviewResult execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND", "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE",
                    "Project must be ACTIVE for sampling preview. Current status: " + project.getStatus());
        }

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0;
        int eligibleCount = populationCount - excludedCount;

        return new SamplingPreviewResult(populationCount, eligibleCount, excludedCount);
    }
}