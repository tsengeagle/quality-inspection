package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RereviewerHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** D-055 第 6 點：匯出只對「需要判斷歷史」的本次案件查詢，不撈與本次匯出無關的案件。 */
@SpringBootTest
class ExportHistoryQueryScopeTest {

    @Autowired private ExportReportUseCase export;
    @Autowired private ReviewCaseJpaRepository cases;
    @SpyBean private RereviewerHistoryQuery historyQuery;

    @BeforeEach
    void seed() {
        cases.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        cases.deleteAll();
    }

    private void save(String id, CaseStatus status, String rereviewerId) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(id); e.setBatchId("b"); e.setProjectId("p"); e.setHospitalId("H001");
        e.setStatus(status); e.setRereviewerId(rereviewerId);
        e.setCreatedAt(java.time.Instant.parse("2026-04-15T00:00:00Z")); // D-057：落在匯出的時間區間內
        cases.save(e);
    }

    private void exportAs(String user, DomainRole role) {
        export.execute(new ActorContext(user, user, EnumSet.of(role), Optional.of("H001")), "REVIEW_COMPLETION", null, java.time.Instant.parse("2026-04-01T00:00:00Z"), java.time.Instant.parse("2026-05-01T00:00:00Z"), 0, 1000);
    }

    @SuppressWarnings("unchecked")
    @Test
    void onlyCasesThatStillNeedAHistoryCheckAreQueried() {
        save("closed", CaseStatus.CLOSED, "rr");
        save("pending", CaseStatus.PENDING_REREVIEW, null);   // 一律遮罩，不必查
        save("own", CaseStatus.CLOSED, "mgr");                 // 匯出者是現任複審醫師，一律遮罩，不必查

        exportAs("mgr", DomainRole.HOSPITAL_MANAGER);

        ArgumentCaptor<Collection<String>> assigned = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection<String>> incomplete = ArgumentCaptor.forClass(Collection.class);
        verify(historyQuery).findCaseIdsAssignedTo(eq("mgr"), assigned.capture());
        verify(historyQuery).findHistoryIncompleteCaseIds(incomplete.capture());
        assertThat(assigned.getValue()).containsExactly("closed");
        assertThat(incomplete.getValue()).containsExactly("closed");
    }

    @Test
    void systemAdmin_andNoCandidates_skipTheQueries() {
        save("closed", CaseStatus.CLOSED, "rr");
        exportAs("admin", DomainRole.SYSTEM_ADMIN);

        save("pending", CaseStatus.PENDING_REREVIEW, null);
        cases.deleteById("closed");
        exportAs("mgr", DomainRole.HOSPITAL_MANAGER);

        verify(historyQuery, never()).findCaseIdsAssignedTo(any(), any());
        verify(historyQuery, never()).findHistoryIncompleteCaseIds(any());
    }
}
