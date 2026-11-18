package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

/** GET /score-sheets/{scoreSheetId}/sections 的回應：{ scoreSheetId, sections }（D-050 第 7 點）。 */
public class ScoreSheetSectionsResponse {

    private final String scoreSheetId;
    private final List<ScoreSheetSectionResponse> sections;

    public ScoreSheetSectionsResponse(String scoreSheetId, List<ScoreSheetSectionResponse> sections) {
        this.scoreSheetId = scoreSheetId;
        this.sections = sections;
    }

    public String getScoreSheetId() { return scoreSheetId; }
    public List<ScoreSheetSectionResponse> getSections() { return sections; }
}
