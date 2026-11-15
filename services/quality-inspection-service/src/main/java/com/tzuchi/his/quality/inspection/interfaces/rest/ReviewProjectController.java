package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.project.*;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/review-projects")
public class ReviewProjectController {

    private final CreateReviewProjectUseCase createUseCase;
    private final PublishReviewProjectVersionUseCase publishUseCase;
    private final UpdateReviewProjectDraftUseCase updateDraftUseCase;
    private final ActivateReviewProjectUseCase activateUseCase;
    private final DeactivateReviewProjectUseCase deactivateUseCase;
    private final BindScoreSheetToProjectUseCase bindScoreSheetUseCase;
    private final ListReviewProjectsUseCase listUseCase;
    private final GetReviewProjectDetailUseCase getDetailUseCase;
    private final GetReviewProjectVersionUseCase getVersionUseCase;

    public ReviewProjectController(CreateReviewProjectUseCase createUseCase,
                                    PublishReviewProjectVersionUseCase publishUseCase,
                                    UpdateReviewProjectDraftUseCase updateDraftUseCase,
                                    ActivateReviewProjectUseCase activateUseCase,
                                    DeactivateReviewProjectUseCase deactivateUseCase,
                                    BindScoreSheetToProjectUseCase bindScoreSheetUseCase,
                                    ListReviewProjectsUseCase listUseCase,
                                    GetReviewProjectDetailUseCase getDetailUseCase,
                                    GetReviewProjectVersionUseCase getVersionUseCase) {
        this.createUseCase = createUseCase;
        this.publishUseCase = publishUseCase;
        this.updateDraftUseCase = updateDraftUseCase;
        this.activateUseCase = activateUseCase;
        this.deactivateUseCase = deactivateUseCase;
        this.bindScoreSheetUseCase = bindScoreSheetUseCase;
        this.listUseCase = listUseCase;
        this.getDetailUseCase = getDetailUseCase;
        this.getVersionUseCase = getVersionUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createProject(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody CreateReviewProjectRequest request) {
        Set<MedicalRecordType> types = parseMedicalRecordTypes(request.getMedicalRecordTypes());
        String projectId = createUseCase.execute(actor, request.getName(),
                request.getDescription(), types, request.getQualityThreshold(),
                request.getInitialReviewWeight(), request.getRereviewWeight());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("projectId", projectId)));
    }

    @PostMapping("/{projectId}/publish")
    public ResponseEntity<ApiResponse<Void>> publishVersion(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        publishUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> updateDraft(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId,
            @Valid @RequestBody UpdateReviewProjectRequest request) {
        Set<MedicalRecordType> types = parseMedicalRecordTypes(request.getMedicalRecordTypes());
        updateDraftUseCase.execute(actor, projectId, request.getName(),
                request.getDescription(), types, request.getQualityThreshold(),
                request.getInitialReviewWeight(), request.getRereviewWeight());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{projectId}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        activateUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{projectId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        deactivateUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{projectId}/bind-score-sheet")
    public ResponseEntity<ApiResponse<Void>> bindScoreSheet(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId,
            @Valid @RequestBody BindScoreSheetRequest request) {
        bindScoreSheetUseCase.execute(actor, projectId, request.getScoreSheetId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewProjectResponse>>> listProjects(
            @CurrentActor ActorContext actor,
            @RequestParam(required = false) String hospitalId) {
        List<ReviewProjectSummary> summaries = listUseCase.execute(actor, hospitalId);
        List<ReviewProjectResponse> responses = summaries.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ReviewProjectResponse>> getProject(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        ReviewProjectSummary summary = getDetailUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(summary)));
    }

    @GetMapping("/{projectId}/version")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersion(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        ProjectVersion version = getVersionUseCase.execute(actor, projectId);
        Map<String, Object> versionMap = new LinkedHashMap<>();
        versionMap.put("versionNumber", version.getVersionNumber());
        versionMap.put("published", version.isPublished());
        return ResponseEntity.ok(ApiResponse.ok(versionMap));
    }

    private Set<MedicalRecordType> parseMedicalRecordTypes(Set<String> raw) {
        if (raw == null) return Collections.emptySet();
        return raw.stream()
                .map(s -> {
                    try { return MedicalRecordType.valueOf(s); }
                    catch (IllegalArgumentException e) {
                        throw new BadRequestException("Invalid medicalRecordType: " + s);
                    }
                })
                .collect(Collectors.toSet());
    }

    private ReviewProjectResponse toResponse(ReviewProjectSummary summary) {
        return new ReviewProjectResponse(
                summary.getProjectId(),
                summary.getHospitalId(),
                summary.getName(),
                summary.getDescription(),
                summary.getStatus().name(),
                summary.getBoundScoreSheetId(),
                summary.getVersionNo(),
                summary.isVersionPublished()
        );
    }
}
