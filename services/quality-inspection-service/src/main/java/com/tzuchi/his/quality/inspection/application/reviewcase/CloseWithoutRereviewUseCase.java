package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.CloseWithoutRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.CaseClosedWithoutRereviewEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseWithoutRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;

    public CloseWithoutRereviewUseCase(ReviewCaseRepository caseRepository,
                                        DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, CloseWithoutRereviewCommand cmd) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        reviewCase.closeWithoutRereview();
        caseRepository.save(reviewCase);
        eventPublisher.publish(new CaseClosedWithoutRereviewEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"finalScore\":%s}", reviewCase.getFinalScore())));
    }
}
