package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;

import java.util.List;
import java.util.stream.Collectors;

/** 測試用：把 AnswerItem 轉成修正 use case 的輸入（D-083 第 6 點）。 */
public final class RevisionTestInputs {

    private RevisionTestInputs() {
    }

    public static List<AnswerInput> of(List<AnswerItem> answers) {
        return answers.stream()
                .map(a -> new AnswerInput(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());
    }
}
