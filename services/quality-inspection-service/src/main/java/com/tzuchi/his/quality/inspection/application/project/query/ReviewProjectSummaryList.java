package com.tzuchi.his.quality.inspection.application.project.query;

import java.util.List;

/** ListReviewProjects 的輸出（specs/review-project/usecases.yaml，D-062 第 1 點）：每一項與 GetReviewProjectDetail 的輸出相同。 */
public class ReviewProjectSummaryList {

    private final List<ReviewProjectSummary> items;

    public ReviewProjectSummaryList(List<ReviewProjectSummary> items) {
        this.items = List.copyOf(items);
    }

    public List<ReviewProjectSummary> getItems() { return items; }
}
