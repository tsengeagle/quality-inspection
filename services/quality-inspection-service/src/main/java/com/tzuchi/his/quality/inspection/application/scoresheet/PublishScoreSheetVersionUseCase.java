package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetVersionPublishedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;
import java.time.Instant;

@Service
@Transactional
public class PublishScoreSheetVersionUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public PublishScoreSheetVersionUseCase(ScoreSheetRepository repository,
                                            HospitalIdResolver hospitalIdResolver,
                                            DomainEventPublisher eventPublisher,
                                           Clock clock,
                                           IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
        this.eventPublisher = eventPublisher;
    }

    public PublishScoreSheetVersionResult execute(ActorContext actor, String scoreSheetId) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheet not found: " + scoreSheetId));
        String hospitalId = hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId());
        actor.requireHospitalScope(hospitalId);

        scoreSheet.publishVersion();
        repository.save(scoreSheet);
        eventPublisher.publish(new ScoreSheetVersionPublishedEvent(this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(), scoreSheetId,
                scoreSheet.getProjectId(), hospitalId));
        return new PublishScoreSheetVersionResult(scoreSheet.getVersionId());
    }
}
