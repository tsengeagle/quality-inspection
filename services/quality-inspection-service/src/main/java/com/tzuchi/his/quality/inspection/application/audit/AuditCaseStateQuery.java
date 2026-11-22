package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;

import java.util.Collection;
import java.util.Map;

/** D-071：稽核遮罩要的案件目前狀態與複審醫師，一次查一頁出現的案件。 */
public interface AuditCaseStateQuery {

    /** 在 caseIds 之中找得到的案件；找不到的不在結果中。 */
    Map<String, CaseState> findStates(Collection<String> caseIds);

    final class CaseState {
        private final CaseStatus status;
        private final String rereviewerId;

        public CaseState(CaseStatus status, String rereviewerId) {
            this.status = status;
            this.rereviewerId = rereviewerId;
        }

        public CaseStatus getStatus() { return status; }
        public String getRereviewerId() { return rereviewerId; }
    }
}
