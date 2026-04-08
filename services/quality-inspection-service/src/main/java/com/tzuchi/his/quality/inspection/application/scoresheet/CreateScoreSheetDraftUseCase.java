package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetDraftCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateScoreSheetDraftUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;
    private final DomainEventPublisher eventPublisher;

    public CreateScoreSheetDraftUseCase(ScoreSheetRepository repository,
                                         HospitalIdResolver hospitalIdResolver,
                                         DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
        this.eventPublisher = eventPublisher;
    }

    public String execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = hospitalIdResolver.resolveByProjectId(projectId);
        actor.requireHospitalScope(hospitalId);

        String scoreSheetId = UUID.randomUUID().toString();
        ScoreSheetDefinition scoreSheet = ScoreSheetDefinition.createDraft(scoreSheetId, projectId);
        repository.save(scoreSheet);
        eventPublisher.publish(new ScoreSheetDraftCreatedEvent(this, actor.getUserId(), actor.getUserName(), scoreSheetId,
                projectId, hospitalId));
        return scoreSheetId;
    }
}
