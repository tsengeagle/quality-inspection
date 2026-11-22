package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
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

/**
 * Retry a failed sampling batch.
 * Creates a NEW batch -- does NOT overwrite the failed batch.
 */
@Service
@Transactional
public class RetrySamplingBatchUseCase {

    private final ReviewProjectRepository projectRepository;
    private final SamplingBatchRepository batchRepository;
    private final ReviewCaseRepository caseRepository;
    private final CandidateSourcePort candidateSource;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public RetrySamplingBatchUseCase(ReviewProjectRepository projectRepository,
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

    public String execute(ActorContext actor, String previousBatchId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = actor.requireHospitalId();
        SamplingBatch previousBatch = batchRepository.findById(previousBatchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + previousBatchId));

        if (previousBatch.getStatus() != BatchStatus.FAILED) {
            throw new DomainRuleViolationException("BATCH_NOT_RETRYABLE",
                    "Only FAILED batches can be retried. Current status: " + previousBatch.getStatus());
        }

        String projectId = previousBatch.getProjectId();
        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE",
                    "Project must be ACTIVE for retry. Current status: " + project.getStatus());
        }

        // D-013: use the PREVIOUS batch's snapshot, not the current project version
        RuleSnapshot snapshot = previousBatch.getRuleSnapshot();

        String newBatchId = UUID.randomUUID().toString();
        SamplingBatch newBatch = SamplingBatch.start(newBatchId, projectId, ExecutionMode.RETRY, snapshot, Instant.now(clock));
        newBatch.markRetryOf(previousBatchId);

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0;
        int eligibleCount = populationCount - excludedCount;

        newBatch.recordCandidateCount(populationCount, eligibleCount, excludedCount);
        newBatch.selectCases(candidates);
        newBatch.complete(Instant.now(clock));

        // Persist state before publishing events — listeners may query the batch
        batchRepository.save(newBatch);

        List<ReviewCase> reviewCases = candidates.stream()
                .map(ref -> ReviewCase.createFromSampling(
                        UUID.randomUUID().toString(),
                        newBatchId,
                        projectId,
                        ref.getHospitalId(),
                        ref.getSourceCaseKey(),
                        ref.getMedicalRecordType()
                ))
                .collect(Collectors.toList());

        caseRepository.saveAll(reviewCases);

        // Publish events after persistence — safe for any listener to query
        eventPublisher.publish(new SamplingBatchStartedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId, projectId, "RETRY"));
        eventPublisher.publish(new SamplingCandidatesPreparedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId,
                populationCount, eligibleCount, excludedCount));
        eventPublisher.publish(new ReviewCasesSelectedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId,
                newBatch.getSelectedCount()));
        eventPublisher.publish(new SamplingBatchCompletedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId, projectId,
                newBatch.getSelectedCount(), populationCount, eligibleCount, excludedCount));

        return newBatchId;
    }
}