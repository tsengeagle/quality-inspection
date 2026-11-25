package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * SYSTEM_ADMIN 讀提交明細：只輸出白名單的欄位（RC-SBV-02，permission-matrix §5.1、D-084 第 4 點）。
 * 白名單以外的欄位不輸出，不是輸出 null（D-088 第 6 點）。
 */
@JsonIgnoreProperties({"answers", "comment", "attachmentRefsJson", "totalScore", "createdAt"})
public class WhitelistedSubmissionDetailView extends SubmissionDetailView {

    public WhitelistedSubmissionDetailView(String submissionId, String caseId, String stage,
                                           int revisionNo, String status, String reviewerId,
                                           Instant submittedAt, List<String> maskedFields) {
        super(submissionId, caseId, stage, revisionNo, status, reviewerId,
                null, null, null, null, null, submittedAt, maskedFields);
    }
}
