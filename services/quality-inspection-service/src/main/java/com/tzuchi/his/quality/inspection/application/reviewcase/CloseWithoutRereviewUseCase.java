package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.CaseClosedWithoutRereviewEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;

@Service
public class CloseWithoutRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public CloseWithoutRereviewUseCase(ReviewCaseRepository caseRepository,
                                        DomainEventPublisher eventPublisher,
                                        Clock clock,
                                       IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void execute(ActorContext actor, String caseId) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.closeWithoutRereview(now);
        caseRepository.save(reviewCase);
        eventPublisher.publish(new CaseClosedWithoutRereviewEvent(
                this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"finalScore\":%s}", reviewCase.getFinalScore())));
    }
}
