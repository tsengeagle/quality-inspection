package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.ExportCaseQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

/**
 * D-057 第 3 點：totalElements 與 summary 對應同一個母體。逐案查詢與統計查詢之間若有新案件提交，
 * 同一次匯出的兩個數字仍必須一致（整次匯出讀同一個快照）。
 */
@SpringBootTest
class ExportSnapshotConsistencyTest {

    private static final Instant FROM = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-05-01T00:00:00Z");

    @Autowired private ExportReportUseCase export;
    @Autowired private ReviewCaseJpaRepository cases;
    @Autowired private PlatformTransactionManager transactions;
    @SpyBean private ExportCaseQuery caseQuery;

    @BeforeEach
    @AfterEach
    void clean() {
        cases.deleteAll();
    }

    private static ReviewCaseEntity caseIn(String id) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(id); e.setBatchId("b"); e.setProjectId("p"); e.setHospitalId("H001");
        e.setStatus(CaseStatus.CLOSED); e.setCreatedAt(FROM.plusSeconds(60));
        return e;
    }

    @Test
    void caseCommittedBetweenRowQueryAndSummary_doesNotSplitThePopulation() {
        cases.save(caseIn("c1"));
        cases.save(caseIn("c2"));
        TransactionTemplate other = new TransactionTemplate(transactions);
        other.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        doAnswer(invocation -> {
            Object page = invocation.callRealMethod();
            other.executeWithoutResult(s -> cases.save(caseIn("c3-committed-in-between")));
            return page;
        }).when(caseQuery).findCases(any(), any(), any(), anyInt(), anyInt());

        ActorContext manager = new ActorContext("mgr", "mgr", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        ExportResult result = export.execute(manager, "REVIEW_COMPLETION", null, FROM, TO, 0, 100);

        assertThat(cases.count()).as("新案件確實已在兩次查詢之間提交").isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getSummary().getTotalCases()).as("summary 與 totalElements 為同一個母體").isEqualTo(2);
    }
}
