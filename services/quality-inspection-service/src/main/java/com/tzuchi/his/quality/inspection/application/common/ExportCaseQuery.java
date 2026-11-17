package com.tzuchi.his.quality.inspection.application.common;

import java.time.Instant;

/**
 * D-057：匯出的逐案查詢。依案件建立時間過濾（from ≤ createdAt < to），依 caseId 遞增排序並分頁；
 * 建立時間為 NULL 的案件不會落在任何區間。院區範圍由呼叫端決定（hospitalId 為 null 表示不限院區）。
 */
public interface ExportCaseQuery {

    Page<ExportCaseRecord> findCases(String hospitalId, Instant from, Instant to, int page, int size);
}
