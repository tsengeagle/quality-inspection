package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.scoresheet.*;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.domain.scoresheet.UpdateDraftCommand;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/score-sheets")
public class ScoreSheetController {

    private final CreateScoreSheetDraftUseCase createDraftUseCase;
    private final PublishScoreSheetVersionUseCase publishVersionUseCase;
    private final UpdateScoreSheetDraftUseCase updateDraftUseCase;
    private final ListScoreSheetSummariesUseCase listUseCase;
    private final GetScoreSheetDetailUseCase detailUseCase;
    private final GetScoreSheetVersionUseCase versionUseCase;
    private final GetScoreSheetSectionsUseCase sectionsUseCase;

    public ScoreSheetController(CreateScoreSheetDraftUseCase createDraftUseCase,
                                 PublishScoreSheetVersionUseCase publishVersionUseCase,
                                 UpdateScoreSheetDraftUseCase updateDraftUseCase,
                                 ListScoreSheetSummariesUseCase listUseCase,
                                 GetScoreSheetDetailUseCase detailUseCase,
                                 GetScoreSheetVersionUseCase versionUseCase,
                                 GetScoreSheetSectionsUseCase sectionsUseCase) {
        this.createDraftUseCase = createDraftUseCase;
        this.publishVersionUseCase = publishVersionUseCase;
        this.updateDraftUseCase = updateDraftUseCase;
        this.listUseCase = listUseCase;
        this.detailUseCase = detailUseCase;
        this.versionUseCase = versionUseCase;
        this.sectionsUseCase = sectionsUseCase;
    }

    @PostMapping
    public ResponseEntity<?> createDraft(@CurrentActor ActorContext actor,
                                          @RequestBody CreateScoreSheetDraftRequest request) {
        String scoreSheetId = createDraftUseCase.execute(actor, request.getProjectId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("scoreSheetId", scoreSheetId)));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishVersion(@CurrentActor ActorContext actor,
                                             @PathVariable("id") String scoreSheetId) {
        publishVersionUseCase.execute(actor, scoreSheetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDraft(@CurrentActor ActorContext actor,
                                          @PathVariable("id") String scoreSheetId,
                                          @RequestBody UpdateScoreSheetDraftRequest request) {
        UpdateDraftCommand command = toCommand(request);
        updateDraftUseCase.execute(actor, scoreSheetId, command);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping
    public ResponseEntity<?> list(@CurrentActor ActorContext actor) {
        List<ScoreSheetSummary> summaries = listUseCase.execute(actor);
        List<ScoreSheetSummaryResponse> response = summaries.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@CurrentActor ActorContext actor,
                                     @PathVariable("id") String scoreSheetId) {
        ScoreSheetSummary summary = detailUseCase.execute(actor, scoreSheetId);
        return ResponseEntity.ok(ApiResponse.ok(toSummaryResponse(summary)));
    }

    @GetMapping("/{id}/version")
    public ResponseEntity<?> version(@CurrentActor ActorContext actor,
                                      @PathVariable("id") String scoreSheetId) {
        ScoreSheetVersionInfo info = versionUseCase.execute(actor, scoreSheetId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "version", info.getVersion(),
                "published", info.isPublished())));
    }

    @GetMapping("/{id}/sections")
    public ResponseEntity<?> sections(@CurrentActor ActorContext actor,
                                       @PathVariable("id") String scoreSheetId) {
        List<ScoreSheetSection> sections = sectionsUseCase.execute(actor, scoreSheetId);
        List<ScoreSheetSectionResponse> response = sections.stream()
                .map(this::toSectionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- Mapping helpers ---

    private UpdateDraftCommand toCommand(UpdateScoreSheetDraftRequest req) {
        List<UpdateDraftCommand.AddSectionCommand> addSections = req.getAddSections() != null
                ? req.getAddSections().stream().map(s -> new UpdateDraftCommand.AddSectionCommand(
                        s.getSectionId(), s.getTitle(),
                        s.getItems() != null ? s.getItems().stream().map(this::toAddItemCmd).collect(Collectors.toList()) : Collections.emptyList()))
                .collect(Collectors.toList()) : null;

        List<UpdateDraftCommand.UpdateSectionCommand> updateSections = req.getUpdateSections() != null
                ? req.getUpdateSections().stream().map(s -> new UpdateDraftCommand.UpdateSectionCommand(
                        s.getSectionId(), s.getTitle()))
                .collect(Collectors.toList()) : null;

        List<UpdateDraftCommand.AddItemCommand> addItems = req.getAddItems() != null
                ? req.getAddItems().stream().map(this::toAddItemCmd).collect(Collectors.toList()) : null;

        List<UpdateDraftCommand.UpdateItemCommand> updateItems = req.getUpdateItems() != null
                ? req.getUpdateItems().stream().map(i -> new UpdateDraftCommand.UpdateItemCommand(
                        i.getItemId(), i.getCode(), i.getTitle(),
                        parseEnum(ScoreType.class, i.getScoreType(), "scoreType"),
                        i.getMaxScore(), i.getWeight(),
                        parseEnum(RequiredRule.class, i.getRequiredRule(), "requiredRule"),
                        i.getConditionExpression()))
                .collect(Collectors.toList()) : null;

        return new UpdateDraftCommand(addSections, updateSections, req.getRemoveSectionIds(),
                addItems, updateItems, req.getRemoveItemIds());
    }

    private UpdateDraftCommand.AddItemCommand toAddItemCmd(UpdateScoreSheetDraftRequest.AddItemRequest i) {
        return new UpdateDraftCommand.AddItemCommand(
                i.getTargetSectionId(), i.getItemId(), i.getCode(), i.getTitle(),
                parseEnum(ScoreType.class, i.getScoreType(), "scoreType"),
                i.getMaxScore(), i.getWeight(),
                parseEnum(RequiredRule.class, i.getRequiredRule(), "requiredRule"),
                i.getConditionExpression());
    }

    private ScoreSheetSummaryResponse toSummaryResponse(ScoreSheetSummary s) {
        return new ScoreSheetSummaryResponse(s.getScoreSheetId(), s.getProjectId(),
                s.getStatus().name(), s.getVersion(), s.getSectionCount(), s.getItemCount());
    }

    private ScoreSheetSectionResponse toSectionResponse(ScoreSheetSection section) {
        List<ScoreSheetItemResponse> items = section.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
        return new ScoreSheetSectionResponse(section.getSectionId(), section.getTitle(), items);
    }

    private ScoreSheetItemResponse toItemResponse(ScoreItem item) {
        return new ScoreSheetItemResponse(item.getItemId(), item.getCode(), item.getTitle(),
                item.getScoreType().name(), item.getMaxScore(), item.getWeight(),
                item.getRequiredRule().name(), item.getConditionExpression());
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException(
                    String.format("Invalid %s: '%s'. Allowed values: %s",
                            fieldName, value,
                            Arrays.stream(enumClass.getEnumConstants())
                                    .map(Enum::name)
                                    .collect(Collectors.joining(", "))));
        }
    }
}
