package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseEscalatedToRereviewEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;
import java.time.Instant;

@Service
public class SendToRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SendToRereviewUseCase(ReviewCaseRepository caseRepository,
                                  DomainEventPublisher eventPublisher,
                                 Clock clock,
                                 IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, String caseId) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.sendToRereview();
        caseRepository.save(reviewCase);
        eventPublisher.publish(ReviewCaseEscalatedToRereviewEvent.manual(
                this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(), reviewCase.getProjectId(), reviewCase.getInitialReviewScore()));
    }
}
