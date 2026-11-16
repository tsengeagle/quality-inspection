package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;

import java.util.List;

/** GetScoreSheetSections 的輸出（specs/score-sheet/usecases.yaml）：評分表 id 與目前版本的章節、題目。 */
public class ScoreSheetSectionsView {

    private final String scoreSheetId;
    private final List<ScoreSheetSection> sections;

    public ScoreSheetSectionsView(String scoreSheetId, List<ScoreSheetSection> sections) {
        this.scoreSheetId = scoreSheetId;
        this.sections = List.copyOf(sections);
    }

    public String getScoreSheetId() { return scoreSheetId; }
    public List<ScoreSheetSection> getSections() { return sections; }
}
