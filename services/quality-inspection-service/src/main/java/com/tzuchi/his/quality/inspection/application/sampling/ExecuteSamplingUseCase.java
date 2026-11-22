package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.*;
import com.tzuchi.his.quality.inspection.domain.sampling.event.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.Clock;
import java.time.Instant;

/** Execute sampling: create batch, snapshot, select cases, create ReviewCases. */
@Service
@Transactional
public class ExecuteSamplingUseCase {

    private final ReviewProjectRepository projectRepository;
    private final SamplingBatchRepository batchRepository;
    private final ReviewCaseRepository caseRepository;
    private final CandidateSourcePort candidateSource;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public ExecuteSamplingUseCase(ReviewProjectRepository projectRepository,
                                  SamplingBatchRepository batchRepository,
                                  ReviewCaseRepository caseRepository,
                                  CandidateSourcePort candidateSource,
                                  DomainEventPublisher eventPublisher,
                                  Clock clock) {
        this.projectRepository = projectRepository;
        this.batchRepository = batchRepository;
        this.caseRepository = caseRepository;
        this.candidateSource = candidateSource;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public String execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = actor.requireHospitalId();
        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND", "Project not found: " + projectId));

        actor.requireHospitalScope(project.getHospitalId());

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE",
                    "Project must be ACTIVE for sampling execution. Current status: " + project.getStatus());
        }
        if (!project.getCurrentVersion().isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Project must have a published version before executing sampling");
        }

        RuleSnapshot snapshot = new RuleSnapshot(
                projectId + "-v" + project.getCurrentVersion().getVersionNumber(),
                project.getCurrentVersion().getVersionNumber(),
                project.getScoreSheetVersionRef() != null ? project.getScoreSheetVersionRef() : "UNBOUND",
                "MANUAL",
                null
        );

        String batchId = UUID.randomUUID().toString();
        SamplingBatch batch = SamplingBatch.start(batchId, projectId, ExecutionMode.MANUAL, snapshot, Instant.now(clock));

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0;
        int eligibleCount = populationCount - excludedCount;

        batch.recordCandidateCount(populationCount, eligibleCount, excludedCount);
        batch.selectCases(candidates);
        batch.complete(Instant.now(clock));

        // Persist state before publishing events — listeners may query the batch
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

        // Publish events after persistence — safe for any listener to query
        eventPublisher.publish(new SamplingBatchStartedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, batchId, projectId, "MANUAL"));
        eventPublisher.publish(new SamplingCandidatesPreparedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, batchId,
                populationCount, eligibleCount, excludedCount));
        eventPublisher.publish(new ReviewCasesSelectedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, batchId,
                batch.getSelectedCount()));
        eventPublisher.publish(new SamplingBatchCompletedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, batchId, projectId,
                batch.getSelectedCount(), populationCount));

        return batchId;
    }
}