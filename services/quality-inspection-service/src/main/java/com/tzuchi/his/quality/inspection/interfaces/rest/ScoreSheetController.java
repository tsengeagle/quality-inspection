package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.scoresheet.*;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.ItemInput;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.ItemToSectionInput;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.SectionInput;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.SectionTitleInput;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        updateDraftUseCase.execute(actor, scoreSheetId,
                toSet(request.getRemoveSectionIds()),
                toSet(request.getRemoveItemIds()),
                map(request.getUpdateSections(), s -> {
                    SectionTitleInput in = new SectionTitleInput();
                    in.setSectionId(s.getSectionId());
                    in.setTitle(s.getTitle());
                    return in;
                }),
                map(request.getUpdateItems(), i -> {
                    ItemInput in = new ItemInput();
                    in.setItemId(i.getItemId());
                    in.setCode(i.getCode());
                    in.setTitle(i.getTitle());
                    in.setScoreType(parseEnum(ScoreType.class, i.getScoreType(), "scoreType"));
                    in.setMaxScore(i.getMaxScore());
                    in.setWeight(i.getWeight());
                    in.setRequiredRule(parseEnum(RequiredRule.class, i.getRequiredRule(), "requiredRule"));
                    in.setConditionExpression(i.getConditionExpression());
                    return in;
                }),
                map(request.getAddSections(), s -> {
                    SectionInput in = new SectionInput();
                    in.setSectionId(s.getSectionId());
                    in.setTitle(s.getTitle());
                    in.setItems(map(s.getItems(), this::toItemInput));
                    return in;
                }),
                map(request.getAddItems(), i -> {
                    ItemInput base = toItemInput(i);
                    ItemToSectionInput in = new ItemToSectionInput();
                    in.setTargetSectionId(i.getTargetSectionId());
                    in.setItemId(base.getItemId());
                    in.setCode(base.getCode());
                    in.setTitle(base.getTitle());
                    in.setScoreType(base.getScoreType());
                    in.setMaxScore(base.getMaxScore());
                    in.setWeight(base.getWeight());
                    in.setRequiredRule(base.getRequiredRule());
                    in.setConditionExpression(base.getConditionExpression());
                    return in;
                }));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping
    public ResponseEntity<?> list(@CurrentActor ActorContext actor,
                                  @RequestParam(required = false) String hospitalId) {
        List<ScoreSheetSummary> summaries = listUseCase.execute(actor, hospitalId).getItems();
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
                "version", info.getVersionNo(),
                "published", info.isPublished())));
    }

    @GetMapping("/{id}/sections")
    public ResponseEntity<?> sections(@CurrentActor ActorContext actor,
                                       @PathVariable("id") String scoreSheetId) {
        List<ScoreSheetSection> sections = sectionsUseCase.execute(actor, scoreSheetId).getSections();
        List<ScoreSheetSectionResponse> response = sections.stream()
                .map(this::toSectionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- Mapping helpers ---

    private ItemInput toItemInput(UpdateScoreSheetDraftRequest.AddItemRequest i) {
        ItemInput in = new ItemInput();
        in.setItemId(i.getItemId());
        in.setCode(i.getCode());
        in.setTitle(i.getTitle());
        in.setScoreType(parseEnum(ScoreType.class, i.getScoreType(), "scoreType"));
        in.setMaxScore(i.getMaxScore());
        in.setWeight(i.getWeight());
        in.setRequiredRule(parseEnum(RequiredRule.class, i.getRequiredRule(), "requiredRule"));
        in.setConditionExpression(i.getConditionExpression());
        return in;
    }

    /** null 元素原樣保留，由 use case 依規格的檢查順序回 VALIDATION_ERROR（不在這裡解參照）。 */
    private static <S, T> List<T> map(List<S> source, java.util.function.Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        List<T> result = new java.util.ArrayList<>(source.size());
        for (S element : source) {
            result.add(element == null ? null : mapper.apply(element));
        }
        return result;
    }

    private static Set<String> toSet(List<String> source) {
        return source == null ? null : new LinkedHashSet<>(source);
    }

    private ScoreSheetSummaryResponse toSummaryResponse(ScoreSheetSummary s) {
        return new ScoreSheetSummaryResponse(s.getScoreSheetId(), s.getProjectId(),
                s.getStatus().name(), s.getVersionNo(), s.getSectionCount(), s.getItemCount());
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

    /** 未提供（null）時回傳 null，交給 domain 依檢查順序回 VALIDATION_ERROR（SS-UPD-04）；不認得的值仍是 BAD_REQUEST。 */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null) {
            return null;
        }
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
