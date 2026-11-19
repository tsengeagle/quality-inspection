package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

/** GET /review-projects 的回應：{ items }（D-062 第 1 點）。 */
public class ReviewProjectListResponse {

    private final List<ReviewProjectResponse> items;

    public ReviewProjectListResponse(List<ReviewProjectResponse> items) {
        this.items = items;
    }

    public List<ReviewProjectResponse> getItems() { return items; }
}
