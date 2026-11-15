package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@Transactional
public class CreateReviewProjectUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final IdGenerator idGenerator;

    public CreateReviewProjectUseCase(ReviewProjectRepository repository,
                                       DomainEventPublisher eventPublisher,
                                       IdGenerator idGenerator) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.idGenerator = idGenerator;
    }

    public String execute(ActorContext actor, String name, String description,
                          Set<MedicalRecordType> medicalRecordTypes, BigDecimal qualityThreshold,
                          BigDecimal initialReviewWeight, BigDecimal rereviewWeight) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = actor.requireHospitalId();
        String projectId = idGenerator.newId();
        ReviewProject project = ReviewProject.create(projectId, hospitalId, name,
                description, medicalRecordTypes, qualityThreshold, initialReviewWeight, rereviewWeight);
        repository.save(project);
        eventPublisher.publish(new ReviewProjectCreatedEvent(this, actor.getUserId(), actor.getUserName(), projectId, hospitalId));
        return projectId;
    }
}
