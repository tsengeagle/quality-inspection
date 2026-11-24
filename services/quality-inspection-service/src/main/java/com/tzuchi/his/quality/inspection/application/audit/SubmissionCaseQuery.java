package com.tzuchi.his.quality.inspection.application.audit;

import java.util.Collection;
import java.util.Map;

/** D-078 第 2 點：由提交編號找所屬案件，一次查一頁出現的提交紀錄。 */
public interface SubmissionCaseQuery {

    /** submissionId → caseId；找不到的不在結果中。 */
    Map<String, String> findCaseIds(Collection<String> submissionIds);
}
