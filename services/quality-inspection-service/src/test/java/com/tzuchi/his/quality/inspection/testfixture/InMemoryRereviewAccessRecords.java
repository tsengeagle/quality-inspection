package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.application.reviewcase.InitialReviewReaderPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.RereviewerAssignmentHistoryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.RereviewerAssignmentRecord;

import java.time.Instant;
import java.util.*;

/** D-055 兩個 port 的記憶體實作；呼叫順序記在 {@link #calls}，供測試驗證「先固定版本、再判斷身分、最後記錄」。 */
public class InMemoryRereviewAccessRecords implements RereviewerAssignmentHistoryPort, InitialReviewReaderPort {

    public final List<RereviewerAssignmentRecord> history = new ArrayList<>();
    public final Map<String, Instant> readers = new LinkedHashMap<>();
    public final Set<String> incompleteCases = new HashSet<>();
    public final List<String> calls = new ArrayList<>();
    public RuntimeException failOnRecord;

    @Override
    public void append(RereviewerAssignmentRecord record) {
        calls.add("append:" + record.getCaseId());
        history.add(record);
    }

    @Override
    public boolean contains(String caseId, String rereviewerId) {
        calls.add("contains:" + caseId);
        return history.stream().anyMatch(r -> r.getCaseId().equals(caseId) && r.getRereviewerId().equals(rereviewerId));
    }

    @Override
    public boolean isComplete(String caseId) {
        calls.add("isComplete:" + caseId);
        return !incompleteCases.contains(caseId);
    }

    public Set<String> findCaseIdsAssignedTo(String rereviewerId) {
        Set<String> ids = new HashSet<>();
        history.stream().filter(r -> r.getRereviewerId().equals(rereviewerId)).forEach(r -> ids.add(r.getCaseId()));
        return ids;
    }

    @Override
    public void pinCaseVersion(String caseId) {
        calls.add("pin:" + caseId);
    }

    @Override
    public boolean hasRead(String caseId, String readerId) {
        return readers.containsKey(caseId + "|" + readerId);
    }

    @Override
    public void record(String caseId, String readerId, Instant readAt) {
        calls.add("record:" + caseId);
        if (failOnRecord != null) throw failOnRecord;
        readers.putIfAbsent(caseId + "|" + readerId, readAt);
    }
}
