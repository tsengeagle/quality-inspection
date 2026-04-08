package com.tzuchi.his.quality.inspection.application.sampling;

/**
 * Wrapper for export result. Actual CSV/Excel conversion deferred to B7.
 */
public class SamplingBatchExportResult {

    private final SamplingBatchDetailView detail;

    public SamplingBatchExportResult(SamplingBatchDetailView detail) {
        this.detail = detail;
    }

    public SamplingBatchDetailView getDetail() { return detail; }
}
