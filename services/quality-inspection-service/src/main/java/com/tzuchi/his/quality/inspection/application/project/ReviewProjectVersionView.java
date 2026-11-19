package com.tzuchi.his.quality.inspection.application.project;

/** GetReviewProjectVersion 的輸出（specs/review-project/usecases.yaml，D-062 第 4 點）：versionNo、published。 */
public class ReviewProjectVersionView {

    private final int versionNo;
    private final boolean published;

    public ReviewProjectVersionView(int versionNo, boolean published) {
        this.versionNo = versionNo;
        this.published = published;
    }

    public int getVersionNo() { return versionNo; }
    public boolean isPublished() { return published; }
}
