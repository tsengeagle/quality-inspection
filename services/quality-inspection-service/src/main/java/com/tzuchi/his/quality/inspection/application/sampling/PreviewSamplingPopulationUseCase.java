package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.SelectedCaseRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public SamplingPreviewResult execute(String projectId) {
        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new IllegalStateException("Project must be ACTIVE for sampling preview");
        }

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0; // simplified — real exclusion logic in future
        int eligibleCount = populationCount - excludedCount;

        List<String> keys = candidates.stream()
                .map(SelectedCaseRef::getSourceCaseKey)
                .collect(Collectors.toList());

        return new SamplingPreviewResult(populationCount, eligibleCount, excludedCount, keys);
    }
}
