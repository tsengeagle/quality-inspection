package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.AssignRereviewerCommand;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewerAssignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignRereviewerUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;

    public AssignRereviewerUseCase(ReviewCaseRepository caseRepository,
                                    DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, AssignRereviewerCommand cmd) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        reviewCase.assignRereviewer(cmd.getRereviewerId(), cmd.getDueAt());
        caseRepository.save(reviewCase);
        eventPublisher.publish(new RereviewerAssignedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"rereviewerId\":\"%s\"}", cmd.getRereviewerId())));
    }
}
