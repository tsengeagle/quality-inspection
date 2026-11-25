package com.tzuchi.his.quality.inspection.infrastructure.persistence;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateRole;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling.SamplingBatchEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling.SamplingEntityFixtures;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet.ScoreItemEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet.ScoreSheetEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet.ScoreSheetSectionEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.staffdirectory.HisStaffDirectoryEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-045 第 5、6 點：schema 由 Flyway 的 migration 建立，以 H2 Oracle 相容模式與 Oracle 方言驗證。
 * 這是正式環境（Oracle 19c 以上）在 CI 上能做到的最接近驗證；無法取代在院內 19c 實際執行 migration。
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:oracle-schema;MODE=Oracle;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=com.tzuchi.his.quality.inspection.infrastructure.persistence.H2OracleCompatibleDialect",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OracleSchemaMigrationTest {

    /**
     * Oracle Database SQL Language Reference, "Oracle SQL Reserved Words"（V$RESERVED_WORDS 中 RESERVED = 'Y'）。
     * H2 的 Oracle 相容模式接受這些字當欄位名，所以要在這裡另外檢查（D-045 第 5 點）。
     */
    private static final Set<String> ORACLE_RESERVED_WORDS = new TreeSet<>(Arrays.asList((
            "ACCESS ADD ALL ALTER AND ANY AS ASC AUDIT BETWEEN BY CHAR CHECK CLUSTER COLUMN COLUMN_VALUE COMMENT "
                    + "COMPRESS CONNECT CREATE CURRENT DATE DECIMAL DEFAULT DELETE DESC DISTINCT DROP ELSE EXCLUSIVE "
                    + "EXISTS FILE FLOAT FOR FROM GRANT GROUP HAVING IDENTIFIED IMMEDIATE IN INCREMENT INDEX INITIAL "
                    + "INSERT INTEGER INTERSECT INTO IS LEVEL LIKE LOCK LONG MAXEXTENTS MINUS MLSLABEL MODE MODIFY "
                    + "NESTED_TABLE_ID NOAUDIT NOCOMPRESS NOT NOWAIT NULL NUMBER OF OFFLINE ON ONLINE OPTION OR ORDER "
                    + "PCTFREE PRIOR PUBLIC RAW RENAME RESOURCE REVOKE ROW ROWID ROWNUM ROWS SELECT SESSION SET SHARE "
                    + "SIZE SMALLINT START SUCCESSFUL SYNONYM SYSDATE TABLE THEN TO TRIGGER UID UNION UNIQUE UPDATE "
                    + "USER VALIDATE VALUES VARCHAR VARCHAR2 VIEW WHENEVER WHERE WITH").split(" ")));

    /** 超過 4000 位元組的中文長文字：VARCHAR2 放不下，必須是 CLOB。 */
    private static final String LONG_TEXT = "初審評語".repeat(1200);

    @Autowired private TestEntityManager em;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void flywayBuildsTheSchemaThatHibernateValidates() {
        // 能執行到這裡，表示 Hibernate 已依 ddl-auto=validate 驗證過 Flyway 建出的 schema。
        List<String> versions = jdbc.queryForList(
                "select \"version\" from \"flyway_schema_history\" order by \"installed_rank\"", String.class);
        assertThat(versions).contains("1");
    }

    @Test
    void noTableOrColumnNameIsAnOracleReservedWord() {
        List<String> names = new ArrayList<>(jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC'", String.class));
        for (var row : jdbc.queryForList(
                "select table_name, column_name from information_schema.columns where table_schema = 'PUBLIC'")) {
            names.add(row.get("TABLE_NAME") + "." + row.get("COLUMN_NAME"));
        }
        List<String> clashes = new ArrayList<>();
        for (String name : names) {
            String last = name.substring(name.lastIndexOf('.') + 1).toUpperCase();
            if (ORACLE_RESERVED_WORDS.contains(last)) {
                clashes.add(name);
            }
        }
        assertThat(names).isNotEmpty();
        assertThat(clashes).as("Oracle reserved words used as table/column names").isEmpty();
    }

    @Test
    void auditTrail_longDetailRoundTrips() {
        AuditTrailEntity e = new AuditTrailEntity();
        e.setId("audit-1");
        e.setEntityType("ReviewCase");
        e.setEntityId("case-1");
        e.setAction("INITIAL_SUBMITTED");
        e.setActorUserId("u1");
        e.setActorUserName("初審醫師");
        e.setHospitalId("H001");
        e.setOccurredAt(Instant.parse("2026-01-01T00:00:00Z"));
        e.setDetail(LONG_TEXT);
        em.persistAndFlush(e);
        em.clear();

        assertThat(em.find(AuditTrailEntity.class, "audit-1").getDetail()).isEqualTo(LONG_TEXT);
    }

    @Test
    void reviewSubmission_commentAndJsonColumnsRoundTrip() {
        ReviewSubmissionEntity s = new ReviewSubmissionEntity();
        s.setSubmissionId("sub-1");
        s.setCaseId("case-1");
        s.setStage(ReviewStage.INITIAL);
        s.setReviewerId("u1");
        s.setStatus(SubmissionStatus.SUBMITTED);
        s.setRevisionNo(1);
        s.setAnswerSet(List.of(AnswerItem.ofScored("Q1", LONG_TEXT, BigDecimal.TEN)));
        s.setTotalScore(new BigDecimal("88.50"));
        s.setComment("評語");
        s.setAttachmentRefsJson(LONG_TEXT);
        s.setCreatedAt(Instant.parse("2026-01-01T08:00:00Z"));
        em.persistAndFlush(s);
        em.clear();

        ReviewSubmissionEntity loaded = em.find(ReviewSubmissionEntity.class, "sub-1");
        assertThat(loaded.getComment()).isEqualTo("評語");
        assertThat(loaded.getAttachmentRefsJson()).isEqualTo(LONG_TEXT);
        assertThat(loaded.getAnswerSet()).hasSize(1);
        assertThat(loaded.getAnswerSet().get(0).getAnswerValue()).isEqualTo(LONG_TEXT);
        assertThat(loaded.getTotalScore()).isEqualByComparingTo("88.50");
    }

    @Test
    void reviewCaseAndProject_roundTripIncludingBooleansAndCollections() {
        ReviewProjectEntity p = new ReviewProjectEntity();
        p.setProjectId("p1");
        p.setHospitalId("H001");
        p.setName("門診病歷品質審查");
        p.setStatus(ProjectStatus.ACTIVE);
        p.setVersionNumber(1);
        p.setVersionPublished(true);
        p.setMedicalRecordTypes(new HashSet<>(List.of("OUTPATIENT", "INPATIENT")));
        p.setInitialReviewWeight(new BigDecimal("0.40"));
        p.setRereviewWeight(new BigDecimal("0.60"));
        em.persist(p);

        ReviewCaseEntity c = new ReviewCaseEntity();
        c.setCaseId("case-1");
        c.setBatchId("b1");
        c.setProjectId("p1");
        c.setHospitalId("H001");
        c.setStatus(CaseStatus.PENDING_REREVIEW);
        c.setInitialReviewScore(new BigDecimal("92.00"));
        em.persistAndFlush(c);
        em.clear();

        ReviewProjectEntity loadedProject = em.find(ReviewProjectEntity.class, "p1");
        assertThat(loadedProject.isVersionPublished()).isTrue();
        assertThat(loadedProject.getMedicalRecordTypes()).containsExactlyInAnyOrder("OUTPATIENT", "INPATIENT");
        ReviewCaseEntity loadedCase = em.find(ReviewCaseEntity.class, "case-1");
        assertThat(loadedCase.getStatus()).isEqualTo(CaseStatus.PENDING_REREVIEW);
    }

    @Test
    void identityColumnsAndForeignKeys_roundTrip() {
        HisStaffDirectoryEntity staff = new HisStaffDirectoryEntity(
                "s1", "王醫師", "H001", AssignmentCandidateRole.REVIEWER, true);
        em.persist(staff);

        ScoreItemEntity item = new ScoreItemEntity();
        item.setItemId("i1");
        item.setCode("C01");
        item.setTitle("題目一");
        item.setScoreType("FIXED");
        item.setMaxScore(BigDecimal.TEN);
        item.setWeight(BigDecimal.ONE);
        item.setRequiredRule("REQUIRED");
        ScoreSheetSectionEntity section = new ScoreSheetSectionEntity();
        section.setSectionId("sec1");
        section.setTitle("一般");
        section.setItems(new ArrayList<>(List.of(item)));
        ScoreSheetEntity sheet = new ScoreSheetEntity();
        sheet.setScoreSheetId("ss-1");
        sheet.setProjectId("p1");
        sheet.setVersion(1);
        sheet.setStatus(ScoreSheetStatus.PUBLISHED);
        sheet.setSections(new ArrayList<>(List.of(section)));
        em.persist(sheet);

        SamplingBatchEntity batch = SamplingEntityFixtures.completedBatchWithOneSelectedCase("b1", "p1");
        em.persistAndFlush(batch);
        em.flush();
        em.clear();

        assertThat(staff.getDirectoryEntryId()).isNotNull();
        ScoreSheetEntity loadedSheet = em.find(ScoreSheetEntity.class, "ss-1");
        assertThat(loadedSheet.getSections()).hasSize(1);
        assertThat(loadedSheet.getSections().get(0).getItems()).extracting(ScoreItemEntity::getCode).containsExactly("C01");
        assertThat(em.find(SamplingBatchEntity.class, "b1").getSelectedCases()).hasSize(1);
    }

    @Test
    void pagedQueryUsesOracleSyntax() {
        for (int i = 1; i <= 3; i++) {
            ReviewCaseEntity c = new ReviewCaseEntity();
            c.setCaseId("case-p" + i);
            c.setBatchId("b1");
            c.setProjectId("p1");
            c.setStatus(CaseStatus.PENDING_INITIAL_REVIEW);
            em.persist(c);
        }
        em.flush();
        em.clear();

        List<ReviewCaseEntity> page = em.getEntityManager()
                .createQuery("select c from ReviewCaseEntity c order by c.caseId", ReviewCaseEntity.class)
                .setFirstResult(1).setMaxResults(1).getResultList();
        assertThat(page).extracting(ReviewCaseEntity::getCaseId).containsExactly("case-p2");
    }
}
