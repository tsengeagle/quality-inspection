package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.project.CreateReviewProjectUseCase;
import com.tzuchi.his.quality.inspection.application.project.PublishReviewProjectVersionUseCase;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateReviewProjectRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/review-projects")
public class ReviewProjectController {

    private final CreateReviewProjectUseCase createUseCase;
    private final PublishReviewProjectVersionUseCase publishUseCase;

    public ReviewProjectController(CreateReviewProjectUseCase createUseCase,
                                   PublishReviewProjectVersionUseCase publishUseCase) {
        this.createUseCase = createUseCase;
        this.publishUseCase = publishUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createProject(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody CreateReviewProjectRequest request) {
        Set<MedicalRecordType> types = request.getMedicalRecordTypes() != null
                ? request.getMedicalRecordTypes().stream()
                    .map(s -> {
                        try { return MedicalRecordType.valueOf(s); }
                        catch (IllegalArgumentException e) {
                            throw new BadRequestException("Invalid medicalRecordType: " + s);
                        }
                    })
                    .collect(Collectors.toSet())
                : Collections.emptySet();

        String projectId = createUseCase.execute(actor, request.getHospitalId(),
                request.getName(), types);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("projectId", projectId)));
    }

    @PostMapping("/{projectId}/publish-version")
    public ResponseEntity<ApiResponse<Void>> publishVersion(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        publishUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
