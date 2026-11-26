package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
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
import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
public class CreateReviewProjectUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public CreateReviewProjectUseCase(ReviewProjectRepository repository,
                                       DomainEventPublisher eventPublisher,
                                       IdGenerator idGenerator,
                                      Clock clock) {
        this.clock = clock;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.idGenerator = idGenerator;
    }

    public String execute(ActorContext actor, String name, String description,
                          Set<String> medicalRecordTypes, BigDecimal qualityThreshold,
                          BigDecimal initialReviewWeight, BigDecimal rereviewWeight) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = actor.requireHospitalId();
        Set<MedicalRecordType> types = parseMedicalRecordTypes(medicalRecordTypes);
        String projectId = idGenerator.newId();
        ReviewProject project = ReviewProject.create(projectId, hospitalId, name,
                description, types, qualityThreshold, initialReviewWeight, rereviewWeight);
        repository.save(project);
        eventPublisher.publish(new ReviewProjectCreatedEvent(this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(), projectId, hospitalId));
        return projectId;
    }

    /** RP-CRT-04、RP-UPD-05（D-089 第 3 點）：不是 MedicalRecordType 的值回 BAD_REQUEST。null 交給 domain（空集合的檢查）。 */
    static Set<MedicalRecordType> parseMedicalRecordTypes(Set<String> raw) {
        if (raw == null) return null;
        Set<MedicalRecordType> types = new java.util.LinkedHashSet<>();
        for (String s : raw) {
            try {
                types.add(MedicalRecordType.valueOf(s));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new DomainValidationException("BAD_REQUEST", "Invalid medicalRecordType: " + s);
            }
        }
        return types;
    }
}
