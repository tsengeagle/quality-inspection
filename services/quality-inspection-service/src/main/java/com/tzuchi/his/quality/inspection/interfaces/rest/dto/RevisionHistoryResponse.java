package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

public class RevisionHistoryResponse {

    private String caseId;
    private List<SubmissionSummaryDto> submissions;
    private boolean hasExceptionEntry;

    public RevisionHistoryResponse() {}

    public RevisionHistoryResponse(String caseId, List<SubmissionSummaryDto> submissions) {
        this.caseId = caseId;
        this.submissions = submissions;
        this.hasExceptionEntry = false;
    }

    public RevisionHistoryResponse(String caseId, List<SubmissionSummaryDto> submissions,
                                   boolean hasExceptionEntry) {
        this.caseId = caseId;
        this.submissions = submissions;
        this.hasExceptionEntry = hasExceptionEntry;
    }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public List<SubmissionSummaryDto> getSubmissions() { return submissions; }
    public void setSubmissions(List<SubmissionSummaryDto> submissions) { this.submissions = submissions; }
    public boolean isHasExceptionEntry() { return hasExceptionEntry; }
    public void setHasExceptionEntry(boolean hasExceptionEntry) { this.hasExceptionEntry = hasExceptionEntry; }
}
