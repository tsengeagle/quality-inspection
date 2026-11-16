package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess.InitialReviewReaderJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess.RereviewerAssignmentHistoryEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess.RereviewerAssignmentHistoryJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** D-055 第 6 點：匯出在案件仍可能指派複審、匯出者是現任或歷史複審醫師、或歷史不完整時遮罩逐案分數。 */
@SpringBootTest
class ExportScoreMaskingIntegrationTest {

    private static final java.util.List<String> MASKED = java.util.List.of("initialScore", "finalScore");

    @Autowired private ExportQueryService export;
    @Autowired private ReviewCaseJpaRepository cases;
    @Autowired private RereviewerAssignmentHistoryJpaRepository history;
    @Autowired private InitialReviewReaderJpaRepository readers;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        cleanUp();
    }

    @AfterEach
    void cleanUp() {
        readers.deleteAll();
        history.deleteAll();
        cases.deleteAll();
    }

    private void save(String id, CaseStatus status, String rereviewerId) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(id); e.setBatchId("b"); e.setProjectId("p"); e.setHospitalId("H001");
        e.setStatus(status); e.setReviewerId("initial"); e.setRereviewerId(rereviewerId);
        e.setInitialReviewScore(new BigDecimal("92")); e.setFinalScore(new BigDecimal("86"));
        cases.save(e);
    }

    private Map<String, ExportRow> exportAs(String user, DomainRole role) {
        ActorContext actor = new ActorContext(user, user, EnumSet.of(role), Optional.of("H001"));
        return export.export(actor, new ExportCriteria("REVIEW_COMPLETION", null, null, null)).getRows().stream()
                .collect(Collectors.toMap(ExportRow::getCaseId, Function.identity()));
    }

    private static void assertMasked(ExportRow row) {
        assertThat(row.getInitialScore()).as(row.getCaseId() + " initialScore").isNull();
        assertThat(row.getFinalScore()).as(row.getCaseId() + " finalScore").isNull();
        assertThat(row.getMaskedFields()).as(row.getCaseId()).containsExactlyElementsOf(MASKED);
    }

    private static void assertShown(ExportRow row) {
        assertThat(row.getInitialScore()).as(row.getCaseId()).isEqualByComparingTo("92");
        assertThat(row.getFinalScore()).as(row.getCaseId()).isEqualByComparingTo("86");
        assertThat(row.getMaskedFields()).as(row.getCaseId()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = CaseStatus.class, names = {"REREVIEW_IN_PROGRESS", "CLOSED", "CANCELLED"})
    void noFurtherAssignmentPossible_scoresShown(CaseStatus status) {
        save("c", status, "rr");
        assertShown(exportAs("mgr", DomainRole.HOSPITAL_MANAGER).get("c"));
        assertShown(exportAs("aud", DomainRole.AUDITOR).get("c"));
    }

    @ParameterizedTest
    @EnumSource(value = CaseStatus.class, names = {"REREVIEW_IN_PROGRESS", "CLOSED", "CANCELLED"}, mode = EnumSource.Mode.EXCLUDE)
    void assignmentStillPossible_scoresMasked(CaseStatus status) {
        save("c", status, null);
        assertMasked(exportAs("mgr", DomainRole.HOSPITAL_MANAGER).get("c"));
    }

    @Test
    void currentRereviewerExportingAsManager_isMasked() {
        save("c", CaseStatus.CLOSED, "rr");
        assertMasked(exportAs("rr", DomainRole.HOSPITAL_MANAGER).get("c"));
    }

    @Test
    void historicalRereviewerExportingAsAuditor_isMaskedOnlyForThatCase() {
        save("c", CaseStatus.CLOSED, "rr2");
        save("other", CaseStatus.CLOSED, "rr2");
        history.save(new RereviewerAssignmentHistoryEntity("c", "rr1", Instant.parse("2026-01-01T00:00:00Z"), "mgr", "ASSIGN"));

        Map<String, ExportRow> rows = exportAs("rr1", DomainRole.AUDITOR);

        assertMasked(rows.get("c"));
        assertShown(rows.get("other"));
    }

    @Test
    void incompleteHistory_isMasked() {
        save("c", CaseStatus.CLOSED, "rr");
        jdbc.update("update review_case set rereviewer_history_complete = 0 where case_id = 'c'");
        assertMasked(exportAs("mgr", DomainRole.HOSPITAL_MANAGER).get("c"));
    }

    @Test
    void systemAdmin_neverGetsPerCaseScores() {
        save("c", CaseStatus.CLOSED, "rr");
        assertMasked(exportAs("admin", DomainRole.SYSTEM_ADMIN).get("c"));
    }

    @Test
    void exportWritesNoReaderRecordsAndDoesNotBumpTheCaseVersion() {
        save("c", CaseStatus.PENDING_REREVIEW, null);
        Long before = cases.findById("c").orElseThrow().getVersion();

        exportAs("mgr", DomainRole.HOSPITAL_MANAGER);

        assertThat(readers.findAll()).isEmpty();
        assertThat(cases.findById("c").orElseThrow().getVersion()).isEqualTo(before);
    }
}
