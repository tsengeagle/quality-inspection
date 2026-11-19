package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.StartRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class StartRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public StartRereviewUseCase(ReviewCaseRepository caseRepository,
                                 ReviewSubmissionRepository submissionRepository,
                                 DomainEventPublisher eventPublisher,
                                 Clock clock) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void execute(ActorContext actor, StartRereviewCommand cmd) {
        actor.requireRole(DomainRole.REREVIEWER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + cmd.getCaseId()));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        // Domain method validates actor is the assigned rereviewer
        reviewCase.startRereview(actor.getUserId());
        ReviewSubmission rereviewDraft = ReviewSubmission.createRereviewDraft(
                UUID.randomUUID().toString(), cmd.getCaseId(), actor.getUserId(), LocalDateTime.now(clock));
        submissionRepository.save(rereviewDraft);
        reviewCase.setLatestRereviewSubmissionId(rereviewDraft.getSubmissionId());
        caseRepository.save(reviewCase);
        eventPublisher.publish(new RereviewStartedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"submissionId\":\"%s\"}", rereviewDraft.getSubmissionId())));
    }
}
