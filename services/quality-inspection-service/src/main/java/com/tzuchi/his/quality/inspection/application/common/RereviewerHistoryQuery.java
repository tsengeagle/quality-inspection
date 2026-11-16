package com.tzuchi.his.quality.inspection.application.common;

import java.util.Collection;
import java.util.Set;

/**
 * D-055 第 6 點：報表／匯出用的唯讀查詢，查一個人曾出現在哪些案件的複審指派歷史中。
 * 放在 application.common，是因為 reporting 只能依賴 application.common（ARCH-05、D-048）。
 */
public interface RereviewerHistoryQuery {

    /** 在 caseIds 之中，這個人曾出現在複審指派歷史的案件。 */
    Set<String> findCaseIdsAssignedTo(String rereviewerId, Collection<String> caseIds);

    /** 在 caseIds 之中，複審指派歷史不完整的案件（D-055 第 2 點：只有 V5 之前已存在的資料）。 */
    Set<String> findHistoryIncompleteCaseIds(Collection<String> caseIds);
}
