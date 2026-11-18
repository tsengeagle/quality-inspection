package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

public class RevisionHistoryResponse {

    private String caseId;
    private List<SubmissionSummaryDto> submissions;

    public RevisionHistoryResponse() {}

    public RevisionHistoryResponse(String caseId, List<SubmissionSummaryDto> submissions) {
        this.caseId = caseId;
        this.submissions = submissions;
    }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public List<SubmissionSummaryDto> getSubmissions() { return submissions; }
    public void setSubmissions(List<SubmissionSummaryDto> submissions) { this.submissions = submissions; }
}
