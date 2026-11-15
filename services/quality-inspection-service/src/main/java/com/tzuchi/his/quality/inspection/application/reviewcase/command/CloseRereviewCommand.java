package com.tzuchi.his.quality.inspection.application.reviewcase.command;

/**
 * 複審的獨立結案命令（D-034、D-053）。{@code submissionId} 是要結案的有效複審結果，執行時不得改用新版。
 */
public class CloseRereviewCommand {
    private String caseId;
    private String submissionId;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }
}
