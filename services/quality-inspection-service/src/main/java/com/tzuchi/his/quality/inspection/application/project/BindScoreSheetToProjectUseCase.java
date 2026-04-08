package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ScoreSheetBoundToProjectEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BindScoreSheetToProjectUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public BindScoreSheetToProjectUseCase(ReviewProjectRepository repository,
                                           DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String projectId, String scoreSheetId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.bindScoreSheet(scoreSheetId);
        repository.save(project);
        eventPublisher.publish(new ScoreSheetBoundToProjectEvent(this, actor, projectId,
                project.getHospitalId(), scoreSheetId));
    }
}