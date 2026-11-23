package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 儀表板的 4 個數字（總數、已結案、有複審、逾期）必須來自同一個母體：以單一查詢一次算出，
 * 單一 SQL 敘述在 READ COMMITTED 下也讀同一個時間點，查詢之間提交的案件不會只算進其中幾個數字。
 */
@SpringBootTest
class DashboardSingleQueryTest {

    @Autowired private DashboardQueryService dashboard;
    @Autowired private ReviewCaseJpaRepository cases;
    @Autowired private EntityManagerFactory emf;

    private final ActorContext manager =
            new ActorContext("mgr", "mgr", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

    @BeforeEach
    @AfterEach
    void clean() {
        cases.deleteAll();
    }

    private void save(String id, CaseStatus status, String rereviewerId, Instant dueAt) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(id); e.setBatchId("b"); e.setProjectId("p"); e.setHospitalId("H001");
        e.setStatus(status); e.setRereviewerId(rereviewerId); e.setDueAt(dueAt);
        e.setCreatedAt(Instant.parse("2026-04-15T00:00:00Z"));
        cases.save(e);
    }

    @Test
    void allFourNumbersComeFromOneStatement() {
        save("c1", CaseStatus.CLOSED, "rr", null);
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            dashboard.getSummary(manager, new DashboardQueryCriteria(null, null, null, null));
            assertThat(stats.getPrepareStatementCount()).as("一次統計只執行一條 SQL").isEqualTo(1);
        } finally {
            stats.setStatisticsEnabled(wasEnabled);
        }
    }

    @Test
    void countsAndRatesAreCorrectForMixedCases() {
        Instant past = Instant.parse("2000-01-01T00:00:00Z");
        Instant future = Instant.parse("2999-01-01T00:00:00Z");
        save("closed-rereviewed", CaseStatus.CLOSED, "rr", past);     // 已結案、有複審；逾期不計（已結案）
        save("closed-plain", CaseStatus.CLOSED, null, null);           // 已結案
        save("overdue", CaseStatus.PENDING_INITIAL_REVIEW, null, past); // 逾期
        save("cancelled-past-due", CaseStatus.CANCELLED, null, past);  // 已取消，不算逾期
        save("in-progress-rereview", CaseStatus.REREVIEW_IN_PROGRESS, "rr2", future); // 有複審、未逾期
        save("no-due", CaseStatus.PENDING_INITIAL_REVIEW, null, null);  // 沒有期限，不算逾期

        DashboardSummary s = dashboard.getSummary(manager, new DashboardQueryCriteria(null, null, null, null));

        assertThat(s.getTotalCases()).isEqualTo(6);
        assertThat(s.getClosedCases()).isEqualTo(2);
        assertThat(s.getRereviewedCases()).isEqualTo(2);
        assertThat(s.getOverdueCases()).isEqualTo(1);
        assertThat(s.getCompletionRate()).isEqualTo(2.0 / 6);
    }

    @Test
    void noMatchingCases_allZero() {
        DashboardSummary s = dashboard.getSummary(manager, new DashboardQueryCriteria(null, null, null, null));
        assertThat(s.getTotalCases()).isZero();
        assertThat(s.getClosedCases()).isZero();
        assertThat(s.getRereviewedCases()).isZero();
        assertThat(s.getOverdueCases()).isZero();
    }
}
