package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
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
    private final IdGenerator idGenerator;

    public RetrySamplingBatchUseCase(ReviewProjectRepository projectRepository,
                                     SamplingBatchRepository batchRepository,
                                     ReviewCaseRepository caseRepository,
                                     CandidateSourcePort candidateSource,
                                     DomainEventPublisher eventPublisher,
                                     Clock clock,
                                     IdGenerator idGenerator) {
        this.projectRepository = projectRepository;
        this.batchRepository = batchRepository;
        this.caseRepository = caseRepository;
        this.candidateSource = candidateSource;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    /**
     * D-086 第 3 點的檢查順序：角色 → 批次不存在 → 批次所屬專案的院區 → 批次不是 FAILED。
     *
     * @param batchId 要重跑的批次
     */
    public String execute(ActorContext actor, String batchId) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String previousBatchId = batchId;
        SamplingBatch previousBatch = batchRepository.findById(previousBatchId)
                .orElseThrow(() -> new ResourceNotFoundException("SAMPLING_BATCH_NOT_FOUND",
                        "SamplingBatch not found: " + previousBatchId));

        String projectId = previousBatch.getProjectId();
        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        String hospitalId = project.getHospitalId();

        if (previousBatch.getStatus() != BatchStatus.FAILED) {
            throw new DomainRuleViolationException("BATCH_NOT_RETRYABLE",
                    "Only FAILED batches can be retried. Current status: " + previousBatch.getStatus());
        }

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE",
                    "Project must be ACTIVE for retry. Current status: " + project.getStatus());
        }

        // D-013: use the PREVIOUS batch's snapshot, not the current project version
        RuleSnapshot snapshot = previousBatch.getRuleSnapshot();

        String newBatchId = idGenerator.newId();
        SamplingBatch newBatch = SamplingBatch.start(newBatchId, projectId, ExecutionMode.RETRY, snapshot, now);
        newBatch.markRetryOf(previousBatchId);

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0;
        int eligibleCount = populationCount - excludedCount;

        newBatch.recordCandidateCount(populationCount, eligibleCount, excludedCount);
        newBatch.selectCases(candidates);
        newBatch.complete(now);

        // Persist state before publishing events — listeners may query the batch
        batchRepository.save(newBatch);

        List<ReviewCase> reviewCases = candidates.stream()
                .map(ref -> ReviewCase.createFromSampling(
                        idGenerator.newId(),
                        newBatchId,
                        projectId,
                        ref.getHospitalId(),
                        ref.getSourceCaseKey(),
                        ref.getMedicalRecordType()
                ))
                .collect(Collectors.toList());

        caseRepository.saveAll(reviewCases);

        // Publish events after persistence — safe for any listener to query
        eventPublisher.publish(new SamplingBatchStartedEvent(this, idGenerator.newId(), now,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId, projectId, "RETRY"));
        eventPublisher.publish(new SamplingCandidatesPreparedEvent(this, idGenerator.newId(), now,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId,
                populationCount, eligibleCount, excludedCount));
        eventPublisher.publish(new ReviewCasesSelectedEvent(this, idGenerator.newId(), now,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId,
                newBatch.getSelectedCount()));
        eventPublisher.publish(new SamplingBatchCompletedEvent(this, idGenerator.newId(), now,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId, projectId,
                newBatch.getSelectedCount(), populationCount, eligibleCount, excludedCount));

        return newBatchId;
    }
}