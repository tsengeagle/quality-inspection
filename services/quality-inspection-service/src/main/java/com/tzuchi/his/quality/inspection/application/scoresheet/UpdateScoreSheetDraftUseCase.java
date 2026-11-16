package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.ItemInput;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.ItemToSectionInput;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.SectionInput;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.SectionTitleInput;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.UpdateDraftCommand;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetDraftUpdatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 以六個相位的命令修改評分表草稿（D-015、D-050 第 5 點）。輸入名稱與 specs/score-sheet/usecases.yaml 相同：
 * 驗收範例的執行器依參數名稱綁定。相位的執行順序由 domain 的 {@link ScoreSheetDefinition#updateDraft} 決定。
 */
@Service
@Transactional
public class UpdateScoreSheetDraftUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;
    private final DomainEventPublisher eventPublisher;

    public UpdateScoreSheetDraftUseCase(ScoreSheetRepository repository,
                                         HospitalIdResolver hospitalIdResolver,
                                         DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String scoreSheetId,
                        Set<String> removeSectionIds,
                        Set<String> removeItemIds,
                        List<SectionTitleInput> updateSections,
                        List<ItemInput> updateItems,
                        List<SectionInput> addSections,
                        List<ItemToSectionInput> addItems) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheet not found: " + scoreSheetId));
        String hospitalId = hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId());
        actor.requireHospitalScope(hospitalId);

        UpdateDraftCommand command = new UpdateDraftCommand(
                convert(addSections, UpdateScoreSheetDraftUseCase::toAddSection),
                convert(updateSections, s -> new UpdateDraftCommand.UpdateSectionCommand(s.getSectionId(), s.getTitle())),
                convert(removeSectionIds, id -> id),
                convert(addItems, UpdateScoreSheetDraftUseCase::toAddItem),
                convert(updateItems, UpdateScoreSheetDraftUseCase::toUpdateItem),
                convert(removeItemIds, id -> id));

        scoreSheet.updateDraft(command);
        repository.save(scoreSheet);
        eventPublisher.publish(new ScoreSheetDraftUpdatedEvent(this, actor.getUserId(), actor.getUserName(), scoreSheetId,
                scoreSheet.getProjectId(), hospitalId));
    }

    /**
     * null 表示這個相位沒有內容。清單裡的 null 元素原樣保留，由 domain 在輪到該相位時以 VALIDATION_ERROR 拒絕：
     * 這樣「已發布」（SS-UPD-01）與前面相位的錯誤會先被回報。
     */
    private static <S, T> List<T> convert(Collection<S> source, Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        List<T> result = new ArrayList<>(source.size());
        for (S element : source) {
            result.add(element == null ? null : mapper.apply(element));
        }
        return result;
    }

    private static UpdateDraftCommand.AddSectionCommand toAddSection(SectionInput s) {
        return new UpdateDraftCommand.AddSectionCommand(s.getSectionId(), s.getTitle(),
                convert(s.getItems(), i -> new UpdateDraftCommand.AddItemCommand(null, i.getItemId(), i.getCode(),
                        i.getTitle(), i.getScoreType(), i.getMaxScore(), i.getWeight(),
                        i.getRequiredRule(), i.getConditionExpression())));
    }

    private static UpdateDraftCommand.AddItemCommand toAddItem(ItemToSectionInput i) {
        return new UpdateDraftCommand.AddItemCommand(i.getTargetSectionId(), i.getItemId(), i.getCode(),
                i.getTitle(), i.getScoreType(), i.getMaxScore(), i.getWeight(),
                i.getRequiredRule(), i.getConditionExpression());
    }

    private static UpdateDraftCommand.UpdateItemCommand toUpdateItem(ItemInput i) {
        return new UpdateDraftCommand.UpdateItemCommand(i.getItemId(), i.getCode(), i.getTitle(),
                i.getScoreType(), i.getMaxScore(), i.getWeight(),
                i.getRequiredRule(), i.getConditionExpression());
    }
}
