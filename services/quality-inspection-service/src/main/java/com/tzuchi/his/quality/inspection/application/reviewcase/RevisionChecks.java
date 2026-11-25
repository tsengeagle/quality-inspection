package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.DraftContent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 修正初審與修正複審共用的輸入檢查（D-083 第 1、2 點）。 */
final class RevisionChecks {

    private RevisionChecks() {
    }

    static List<AnswerItem> rawAnswers(List<AnswerInput> answers) {
        if (answers == null) {
            return Collections.emptyList();
        }
        return answers.stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());
    }

    /** 與提交相同（D-067、D-076）：評分表的必答題沒有出現在輸入的 answers 裡就拒絕。 */
    static void requireRequiredItemsAnswered(List<AnswerItem> answers, ScoreSheetDefinition scoreSheet) {
        Set<String> answeredItemIds = answers.stream()
                .map(AnswerItem::getItemId)
                .collect(Collectors.toSet());
        List<String> missingRequired = scoreSheet.getSections().stream()
                .flatMap(s -> s.getItems().stream())
                .filter(item -> item.getRequiredRule() == RequiredRule.ALWAYS_REQUIRED)
                .map(ScoreItem::getItemId)
                .filter(id -> !answeredItemIds.contains(id))
                .collect(Collectors.toList());
        if (!missingRequired.isEmpty()) {
            throw new DomainRuleViolationException("REQUIRED_ITEMS_NOT_ANSWERED",
                    "Required items not answered: " + String.join(", ", missingRequired));
        }
    }

    /** 比照 D-038：answers 與 comment 都和被修正的提交相同就拒絕（只比答案與評語，分數由系統計算）。 */
    static void rejectIfUnchanged(List<AnswerItem> answers, String comment, ReviewSubmission submission) {
        List<AnswerItem> current = submission.getAnswerSet().stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());
        if (new DraftContent(answers, null, comment)
                .sameAs(new DraftContent(current, null, submission.getComment()))) {
            throw new DomainStateConflictException("NO_CHANGES", "Revision content is unchanged");
        }
    }
}
