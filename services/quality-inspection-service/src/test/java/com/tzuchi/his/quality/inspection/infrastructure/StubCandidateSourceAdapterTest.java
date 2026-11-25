package com.tzuchi.his.quality.inspection.infrastructure;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.sampling.SelectedCaseRef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** D-086 第 5 點：替身的母體每次 20 筆，sourceCaseKey 不與先前回傳的重複。 */
class StubCandidateSourceAdapterTest {

    @Test
    void everyQueryReturnsTwentyCandidatesWithKeysNeverSeenBefore() {
        StubCandidateSourceAdapter stub = new StubCandidateSourceAdapter();
        Set<String> keys = new HashSet<>();

        for (int call = 0; call < 3; call++) {
            List<SelectedCaseRef> candidates = stub.queryCandidates(
                    "p1", "H001", List.of(MedicalRecordType.OUTPATIENT));
            assertEquals(StubCandidateSourceAdapter.CANDIDATES_PER_QUERY, candidates.size());
            assertEquals(20, candidates.size());
            for (SelectedCaseRef candidate : candidates) {
                assertTrue(keys.add(candidate.getSourceCaseKey()), "重複的 sourceCaseKey：" + candidate.getSourceCaseKey());
                assertEquals("H001", candidate.getHospitalId());
                assertEquals(MedicalRecordType.OUTPATIENT, candidate.getMedicalRecordType());
            }
        }
        assertEquals(60, keys.size());
    }

    @Test
    void aRestartedStubDoesNotRepeatEarlierKeys() throws InterruptedException {
        Set<String> keys = new HashSet<>();
        for (SelectedCaseRef c : new StubCandidateSourceAdapter().queryCandidates("p1", "H001", List.of())) {
            keys.add(c.getSourceCaseKey());
        }
        Thread.sleep(30); // 流水號從啟動時間（毫秒）開始，前一個只用掉 20 個號碼
        for (SelectedCaseRef c : new StubCandidateSourceAdapter().queryCandidates("p1", "H001", List.of())) {
            assertTrue(keys.add(c.getSourceCaseKey()));
        }
    }
}