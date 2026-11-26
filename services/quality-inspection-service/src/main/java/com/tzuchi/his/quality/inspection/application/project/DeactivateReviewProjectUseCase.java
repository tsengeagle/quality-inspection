package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectDeactivatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;
import java.time.Instant;

@Service
@Transactional
public class DeactivateReviewProjectUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public DeactivateReviewProjectUseCase(ReviewProjectRepository repository,
                                           DomainEventPublisher eventPublisher,
                                          Clock clock,
                                          IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String projectId) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.deactivate();
        repository.save(project);
        eventPublisher.publish(new ReviewProjectDeactivatedEvent(this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(), projectId,
                project.getHospitalId()));
    }
}