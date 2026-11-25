package com.tzuchi.his.quality.inspection.domain.reviewcase;

/**
 * Test-only：建立測試情境用。正式程式只經 startRereview／recordRereviewRevision 改變複審指標（D-083 第 9 點）。
 */
public final class ReviewCaseTestSupport {

    private ReviewCaseTestSupport() {
    }

    public static void pointRereviewAt(ReviewCase reviewCase, String submissionId) {
        reviewCase.setLatestRereviewSubmissionId(submissionId);
    }
}
