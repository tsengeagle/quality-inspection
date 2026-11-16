package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.AssignRereviewerUseCase;
import com.tzuchi.his.quality.inspection.application.reviewcase.GetReviewCaseDetailUseCase;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.AssignRereviewerCommand;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.*;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess.*;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D-055：以複審指派歷史證明「不是歷史複審醫師」、初審讀取者紀錄，以及讀取與指派互斥。
 * 使用真實 JPA、Flyway schema 與 HTTP 入口；交易都會 commit，結束後清掉。
 */
@SpringBootTest
@AutoConfigureMockMvc
class RereviewerHistoryReadAccessApiTest {

    private static final String[] FULL_READ_PATHS = {"/review-submissions/i1", "/review-submissions/i2",
            "/review-cases/c1", "/review-cases/c1/revision-history"};

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ReviewCaseJpaRepository cases;
    @Autowired private ReviewSubmissionJpaRepository submissions;
    @Autowired private ReviewProjectJpaRepository projects;
    @Autowired private AuditTrailJpaRepository audit;
    @Autowired private RereviewerAssignmentHistoryJpaRepository history;
    @Autowired private InitialReviewReaderJpaRepository readers;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ReviewCaseRepository caseRepository;
    @Autowired private AssignRereviewerUseCase assign;
    @Autowired private GetReviewCaseDetailUseCase detail;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    void seed() {
        cleanUp();
        ReviewProjectEntity project = new ReviewProjectEntity();
        project.setProjectId("p"); project.setName("Project"); project.setHospitalId("H001");
        project.setStatus(ProjectStatus.ACTIVE);
        projects.save(project);
        ReviewCaseEntity rc = new ReviewCaseEntity();
        rc.setCaseId("c1"); rc.setBatchId("b"); rc.setProjectId("p"); rc.setHospitalId("H001");
        rc.setStatus(CaseStatus.PENDING_REREVIEW); rc.setReviewerId("initial");
        rc.setReviewerAssignedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        rc.setInitialReviewScore(new BigDecimal("92")); rc.setFinalScore(new BigDecimal("92"));
        rc.setLatestInitialSubmissionId("i2");
        cases.save(rc);
        submissions.save(sub("i1", SubmissionStatus.SUPERSEDED));
        submissions.save(sub("i2", SubmissionStatus.SUBMITTED));
    }

    @AfterEach
    void cleanUp() {
        readers.deleteAll();
        history.deleteAll();
        audit.deleteAll();
        submissions.deleteAll();
        cases.deleteAll();
    }

    private static ReviewSubmissionEntity sub(String id, SubmissionStatus state) {
        ReviewSubmissionEntity s = new ReviewSubmissionEntity();
        s.setSubmissionId(id); s.setCaseId("c1"); s.setStage(ReviewStage.INITIAL); s.setReviewerId("initial");
        s.setStatus(state); s.setRevisionNo(id.equals("i2") ? 2 : 1);
        s.setAnswerSet(List.of(AnswerItem.ofScored("Q", "SECRET_INITIAL_ANSWER", BigDecimal.TEN)));
        s.setTotalScore(new BigDecimal("92")); s.setComment("SECRET_INITIAL_COMMENT");
        s.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0)); s.setSubmittedAt(s.getCreatedAt());
        if (state == SubmissionStatus.SUPERSEDED) s.setSupersededBySubmissionId("i2");
        return s;
    }

    private String token(String user, String... roles) throws Exception {
        List<Map<String, Object>> roleClaims = new ArrayList<>();
        for (String r : roles) roleClaims.add(Map.of("role_name", r, "enabled", true));
        String tzUser = mapper.writeValueAsString(Map.of("user", Map.of("user_id", user,
                "user_name", user, "hospital_id", "H001", "user_role", roleClaims)));
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString("{\"alg\":\"RS512\",\"typ\":\"JWT\"}".getBytes()) + "."
                + b64.encodeToString(mapper.writeValueAsBytes(Map.of("tz-user", tzUser))) + ".test-sig";
    }

    private void historyRow(String rereviewerId) {
        history.save(new RereviewerAssignmentHistoryEntity("c1", rereviewerId,
                Instant.parse("2026-01-02T00:00:00Z"), "mgr", "ASSIGN"));
    }

    private org.springframework.test.web.servlet.ResultActions assignViaApi(String kind, String rereviewerId) throws Exception {
        return mvc.perform(post("/review-cases/c1/" + kind).header("api-key", token("mgr", "hospital_manager"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rereviewerId\":\"" + rereviewerId + "\",\"dueAt\":\"2026-12-31T23:59:00\"}"));
    }

    private static ActorContext manager(String id) {
        return new ActorContext(id, id, EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    // ---- 第 3 點：歷史完整且不在歷史中 → 可完整讀取 ----

    @ParameterizedTest
    @ValueSource(strings = {"hospital_manager", "auditor", "reviewer"})
    void completeHistory_singleRoleFullReadersCanReadAllPaths(String role) throws Exception {
        String user = role.equals("reviewer") ? "initial" : "reader";
        for (String path : FULL_READ_PATHS) {
            mvc.perform(get(path).header("api-key", token(user, role))).andExpect(status().isOk());
        }
        JsonNode body = mapper.readTree(mvc.perform(get("/review-submissions/i2").header("api-key", token(user, role)))
                .andReturn().getResponse().getContentAsString()).path("data");
        assertThat(body.toString()).contains("SECRET_INITIAL_ANSWER");
        assertThat(readers.findAll()).extracting(InitialReviewReaderEntity::getReaderId).containsExactly(user);
    }

    @Test
    void historicalRereviewer_isDeniedOnAllPaths() throws Exception {
        historyRow("old-rr");
        for (String path : FULL_READ_PATHS) {
            mvc.perform(get(path).header("api-key", token("old-rr", "auditor")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        }
        assertThat(readers.findAll()).isEmpty();
    }

    @Test
    void incompleteHistory_isDeniedOnAllPaths() throws Exception {
        jdbc.update("update review_case set rereviewer_history_complete = 0 where case_id = 'c1'");
        for (String path : FULL_READ_PATHS) {
            mvc.perform(get(path).header("api-key", token("reader", "hospital_manager")))
                    .andExpect(status().isForbidden());
        }
        assertThat(readers.findAll()).isEmpty();
    }

    // ---- 第 2 點：完整性旗標 ----

    @Test
    void casesCreatedThroughTheEntityAreComplete_rowsWithoutTheColumnDefaultToIncomplete() {
        assertThat(cases.findById("c1").orElseThrow().isRereviewerHistoryComplete()).isTrue();
        // 不指定這個欄位的 insert（等同 V5 之前就存在的資料）取得 migration 的預設值：不完整。
        jdbc.update("insert into review_case (case_id, batch_id, project_id, status, rereview_required)"
                + " values ('legacy', 'b', 'p', 'CLOSED', 0)");
        assertThat(jdbc.queryForObject("select rereviewer_history_complete from review_case where case_id = 'legacy'",
                Integer.class)).isZero();
        assertThat(cases.findById("legacy").orElseThrow().isRereviewerHistoryComplete()).isFalse();
    }

    // ---- 第 5 點：讀取者紀錄 ----

    @Test
    void readerRecordedOnce_caseVersionBumpedOnce_updatedAtUnchanged() throws Exception {
        ReviewCaseEntity before = cases.findById("c1").orElseThrow();
        String auth = token("reader", "hospital_manager");

        mvc.perform(get("/review-cases/c1").header("api-key", auth)).andExpect(status().isOk());
        mvc.perform(get("/review-cases/c1/revision-history").header("api-key", auth)).andExpect(status().isOk());

        assertThat(readers.findAll()).hasSize(1);
        InitialReviewReaderEntity r = readers.findAll().get(0);
        assertThat(r.getCaseId()).isEqualTo("c1");
        assertThat(r.getReadAt()).isNotNull();
        ReviewCaseEntity after = cases.findById("c1").orElseThrow();
        assertThat(after.getVersion()).isEqualTo(before.getVersion() + 1);
        assertThat(after.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
    }

    @Test
    void rereviewerAndSystemAdminReads_produceNoReaderRecords() throws Exception {
        assignViaApi("assign-rereviewer", "rr").andExpect(status().isOk());
        mvc.perform(get("/review-cases/c1").header("api-key", token("rr", "rereviewer"))).andExpect(status().isOk());
        mvc.perform(get("/review-cases/c1").header("api-key", token("admin", "system_admin"))).andExpect(status().isOk());
        assertThat(readers.findAll()).isEmpty();
    }

    @Test
    void readerCannotBeAssignedOrReassigned_422() throws Exception {
        mvc.perform(get("/review-cases/c1").header("api-key", token("reader", "auditor"))).andExpect(status().isOk());

        assignViaApi("assign-rereviewer", "reader").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("REREVIEWER_HAS_READ_INITIAL"));
        assertThat(cases.findById("c1").orElseThrow().getRereviewerId()).isNull();

        assignViaApi("assign-rereviewer", "rr").andExpect(status().isOk());
        assignViaApi("reassign-rereviewer", "reader").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("REREVIEWER_HAS_READ_INITIAL"));
        assertThat(cases.findById("c1").orElseThrow().getRereviewerId()).isEqualTo("rr");
    }

    // ---- 第 1、4 點：指派與改派寫入歷史 ----

    @Test
    void assignAndReassign_appendHistory_andFormerRereviewerStaysHistorical() throws Exception {
        assignViaApi("assign-rereviewer", "rr1").andExpect(status().isOk());
        assignViaApi("reassign-rereviewer", "rr2").andExpect(status().isOk());

        assertThat(history.findAll()).extracting(RereviewerAssignmentHistoryEntity::getRereviewerId,
                RereviewerAssignmentHistoryEntity::getKind, RereviewerAssignmentHistoryEntity::getAssignedBy)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("rr1", "ASSIGN", "mgr"),
                        org.assertj.core.groups.Tuple.tuple("rr2", "REASSIGN", "mgr"));
        for (String path : FULL_READ_PATHS) {
            mvc.perform(get(path).header("api-key", token("rr1", "hospital_manager"))).andExpect(status().isForbidden());
        }
    }

    @Test
    void rejectedAssignment_writesNoHistory() throws Exception {
        assignViaApi("assign-rereviewer", "initial").andExpect(status().isUnprocessableEntity());
        assertThat(history.findAll()).isEmpty();
    }

    // ---- 第 5 點：讀取與指派互斥（D-045 案件版本） ----

    /** 讀取在判斷身分前已讀入案件；判斷到 commit 之間有指派先 commit → 讀取 commit 時以版本衝突拒絕。 */
    @Test
    void assignmentCommittedDuringRead_readFailsOnTheCaseVersion() {
        TransactionTemplate outer = new TransactionTemplate(transactions);
        TransactionTemplate inner = new TransactionTemplate(transactions);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(s -> {
            cases.findById("c1").orElseThrow(); // 讀取交易先讀入案件
            inner.executeWithoutResult(t -> assign.execute(manager("mgr"), assignCommand("rr")));
            detail.execute(manager("reader"), "c1");
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(readers.findAll()).isEmpty();
        assertThat(cases.findById("c1").orElseThrow().getRereviewerId()).isEqualTo("rr");
    }

    /** 指派先讀入案件；讀取者紀錄先 commit → 過時的指派 commit 時以版本衝突拒絕，不留下歷史。 */
    @Test
    void readCommittedDuringAssignment_staleAssignmentFailsOnTheCaseVersion() {
        TransactionTemplate outer = new TransactionTemplate(transactions);
        TransactionTemplate inner = new TransactionTemplate(transactions);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(s -> {
            caseRepository.findById("c1").orElseThrow(); // 指派交易先讀入案件
            inner.executeWithoutResult(t -> detail.execute(manager("reader"), "c1"));
            assign.execute(manager("mgr"), assignCommand("rr"));
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(readers.findAll()).extracting(InitialReviewReaderEntity::getReaderId).containsExactly("reader");
        assertThat(history.findAll()).isEmpty();
        assertThat(cases.findById("c1").orElseThrow().getRereviewerId()).isNull();
    }

    private static AssignRereviewerCommand assignCommand(String rereviewerId) {
        AssignRereviewerCommand cmd = new AssignRereviewerCommand();
        cmd.setCaseId("c1");
        cmd.setRereviewerId(rereviewerId);
        cmd.setDueAt(LocalDateTime.of(2026, 12, 31, 23, 59));
        return cmd;
    }
}
