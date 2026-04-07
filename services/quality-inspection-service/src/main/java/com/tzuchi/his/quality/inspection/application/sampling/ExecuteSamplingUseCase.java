package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Execute sampling: create batch, snapshot, select cases, create ReviewCases. */
@Service
@Transactional
public class ExecuteSamplingUseCase {

    private final ReviewProjectRepository projectRepository;
    private final SamplingBatchRepository batchRepository;
    private final ReviewCaseRepository caseRepository;
    private final CandidateSourcePort candidateSource;

    public ExecuteSamplingUseCase(ReviewProjectRepository projectRepository,
                                  SamplingBatchRepository batchRepository,
                                  ReviewCaseRepository caseRepository,
                                  CandidateSourcePort candidateSource) {
        this.projectRepository = projectRepository;
        this.batchRepository = batchRepository;
        this.caseRepository = caseRepository;
        this.candidateSource = candidateSource;
    }

    public String execute(String projectId) {
        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE",
                    "Project must be ACTIVE for sampling execution. Current status: " + project.getStatus());
        }
        if (!project.getCurrentVersion().isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Project must have a published version before executing sampling");
        }

        RuleSnapshot snapshot = new RuleSnapshot(
                "project=" + projectId + ",version=" + project.getCurrentVersion().getVersionNumber(),
                "default-sampling-policy"
        );

        String batchId = UUID.randomUUID().toString();
        SamplingBatch batch = SamplingBatch.start(batchId, projectId, ExecutionMode.MANUAL, snapshot);

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0;
        int eligibleCount = populationCount - excludedCount;

        batch.recordCandidateCount(populationCount, eligibleCount, excludedCount);
        batch.selectCases(candidates);
        batch.complete();

        batchRepository.save(batch);

        List<ReviewCase> reviewCases = candidates.stream()
                .map(ref -> ReviewCase.createFromSampling(
                        UUID.randomUUID().toString(),
                        batchId,
                        projectId,
                        ref.getHospitalId(),
                        ref.getSourceCaseKey(),
                        ref.getMedicalRecordType()))
                .collect(Collectors.toList());

        caseRepository.saveAll(reviewCases);

        return batchId;
    }
}