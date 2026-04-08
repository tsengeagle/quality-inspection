package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateReviewProjectUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public CreateReviewProjectUseCase(ReviewProjectRepository repository,
                                       DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public String execute(ActorContext actor, String name, String description,
                          Set<MedicalRecordType> medicalRecordTypes) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = actor.requireHospitalId();
        String projectId = UUID.randomUUID().toString();
        ReviewProject project = ReviewProject.create(projectId, hospitalId, name,
                description, medicalRecordTypes);
        repository.save(project);
        eventPublisher.publish(new ReviewProjectCreatedEvent(this, actor, projectId, hospitalId));
        return projectId;
    }
}
