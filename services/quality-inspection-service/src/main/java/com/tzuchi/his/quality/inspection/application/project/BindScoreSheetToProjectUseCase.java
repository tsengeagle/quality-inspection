package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ScoreSheetBoundToProjectEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 把已發布的評分表版本綁定到專案目前的草稿版本（D-041；specs/review-project BindScoreSheetToProject）。
 * 檢查順序依 D-050 第 8 點：角色 → 專案存在 → 院區 → 專案版本未發布 → 評分表版本存在且屬於本專案 → 評分表版本已發布。
 */
@Service
@Transactional
public class BindScoreSheetToProjectUseCase {

    private final ReviewProjectRepository repository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;

    public BindScoreSheetToProjectUseCase(ReviewProjectRepository repository,
                                           ScoreSheetRepository scoreSheetRepository,
                                           DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String projectId, String scoreSheetVersionId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.requireBindable();

        // 不存在與屬於其他專案的結果相同，不透露其他專案的版本是否存在（RP-BND-02）
        ScoreSheetDefinition scoreSheet = scoreSheetRepository.findByVersionId(scoreSheetVersionId)
                .filter(ss -> projectId.equals(ss.getProjectId()))
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "Score sheet version not found: " + scoreSheetVersionId));
        if (scoreSheet.getStatus() != ScoreSheetStatus.PUBLISHED) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Score sheet version is not published: " + scoreSheetVersionId);
        }

        project.bindScoreSheet(scoreSheetVersionId);
        repository.save(project);
        eventPublisher.publish(new ScoreSheetBoundToProjectEvent(this, actor.getUserId(), actor.getUserName(), projectId,
                project.getHospitalId(), scoreSheetVersionId));
    }
}
