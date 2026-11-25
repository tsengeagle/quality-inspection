package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-042、D-047：報表與匯出的院區範圍一律取自登入者，請求帶的院區參數不採用；
 * 只有 SYSTEM_ADMIN 可以跨院區。
 *
 * <p>資料：H001 兩筆案件、H002 一筆案件。
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportHospitalScopeIntegrationTest {

    @Autowired private GetDashboardSummaryUseCase dashboard;
    @Autowired private ExportReportUseCase export;
    @Autowired private ReviewCaseJpaRepository cases;

    @BeforeEach
    void seed() {
        cases.deleteAll();
        cases.save(reviewCase("h1-a", "H001"));
        cases.save(reviewCase("h1-b", "H001"));
        cases.save(reviewCase("h2-a", "H002"));
    }

    @Test
    void dashboard_managerWithoutHospitalParameter_countsOnlyOwnHospital_D042() {
        DashboardSummary summary = dashboard.execute(managerOf("H001"), null, null, null, null);

        assertThat(summary.getTotalCases()).as("H001 的主管不帶院區參數時看到的案件數").isEqualTo(2);
    }

    @Test
    void dashboard_managerRequestingOtherHospital_stillCountsOnlyOwnHospital_D042() {
        DashboardSummary summary = dashboard.execute(managerOf("H001"), "H002", null, null, null);

        assertThat(summary.getTotalCases()).as("H001 的主管指定 H002 時看到的案件數").isEqualTo(2);
    }

    @Test
    void export_managerWithoutHospitalParameter_returnsOnlyOwnHospital_D042() {
        ExportResult result = export.execute(managerOf("H001"), "REVIEW_COMPLETION", null, java.time.Instant.parse("2026-04-01T00:00:00Z"), java.time.Instant.parse("2026-05-01T00:00:00Z"), 0, 1000);

        assertThat(result.getRows()).extracting(ExportRow::getHospitalId).containsOnly("H001");
    }

    @Test
    void export_managerRequestingOtherHospital_returnsOnlyOwnHospital_D042() {
        ExportResult result = export.execute(managerOf("H001"), "REVIEW_COMPLETION", "H002", java.time.Instant.parse("2026-04-01T00:00:00Z"), java.time.Instant.parse("2026-05-01T00:00:00Z"), 0, 1000);

        assertThat(result.getRows()).extracting(ExportRow::getHospitalId).containsOnly("H001");
    }

    @Test
    void dashboard_systemAdminWithoutHospitalParameter_countsAllHospitals_D047() {
        ActorContext admin = new ActorContext("admin", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        DashboardSummary summary = dashboard.execute(admin, null, null, null, null);

        assertThat(summary.getTotalCases()).isEqualTo(3);
    }

    private static ActorContext managerOf(String hospitalId) {
        return new ActorContext("m-" + hospitalId, "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(hospitalId));
    }

    private static ReviewCaseEntity reviewCase(String caseId, String hospitalId) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(caseId);
        e.setBatchId("b");
        e.setProjectId("p");
        e.setHospitalId(hospitalId);
        e.setStatus(CaseStatus.PENDING_INITIAL_REVIEW);
        e.setCreatedAt(java.time.Instant.parse("2026-04-15T00:00:00Z")); // D-057：落在匯出的時間區間內
        return e;
    }
}
