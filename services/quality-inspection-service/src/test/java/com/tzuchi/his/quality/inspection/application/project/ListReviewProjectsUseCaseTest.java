package com.tzuchi.his.quality.inspection.application.project;

import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.ParameterizedTest;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ListReviewProjectsUseCaseTest {

    private InMemoryQueryPort queryPort;
    private ListReviewProjectsUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryQueryPort();
        useCase = new ListReviewProjectsUseCase(queryPort);
    }

    @Test
    void execute_hospitalManager_shouldReturnOwnHospitalOnly() {
        queryPort.summaries.add(summary("p1", "H001"));
        queryPort.summaries.add(summary("p2", "H002"));
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<ReviewProjectSummary> result = useCase.execute(actor, null);

        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).getProjectId());
    }

    @Test
    void execute_systemAdmin_shouldReturnAll() {
        queryPort.summaries.add(summary("p1", "H001"));
        queryPort.summaries.add(summary("p2", "H002"));
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        List<ReviewProjectSummary> result = useCase.execute(actor, null);

        assertEquals(2, result.size());
    }

    @Test
    void execute_noValidRole_shouldThrow() {
        // REVIEWER／REREVIEWER 的唯讀範圍未定義（Q-12），在定義前不開放
        ActorContext actor = new ActorContext("u1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor, null));
    }

    @ParameterizedTest
    @EnumSource(value = DomainRole.class, names = {"REVIEWER", "REREVIEWER"})
    void execute_reviewerRoles_areDenied(DomainRole role) {
        // D-043：REVIEWER／REREVIEWER 在定義前不開放
        queryPort.summaries.add(summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", role.name(), EnumSet.of(role), Optional.of("H001"));

        RoleDeniedException e = assertThrows(RoleDeniedException.class, () -> useCase.execute(actor, null));
        assertEquals("ROLE_DENIED", e.getErrorCode());
    }

    @Test
    void execute_auditor_shouldReturnOwnHospitalOnly() {
        // api-spec.md §7.7：AUDITOR 可以查詢專案清單；範圍是自己的院區（D-042）
        queryPort.summaries.add(summary("p1", "H001"));
        queryPort.summaries.add(summary("p2", "H002"));
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H001"));

        List<ReviewProjectSummary> result = useCase.execute(actor, null);

        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).getProjectId());
    }

    @Test
    void execute_systemAdmin_withHospitalId_shouldNarrowToThatHospital() {
        // D-047 第 1 點：SYSTEM_ADMIN 可以用院區參數縮小範圍
        queryPort.summaries.add(summary("p1", "H001"));
        queryPort.summaries.add(summary("p2", "H002"));
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        List<ReviewProjectSummary> result = useCase.execute(actor, "H002");

        assertEquals(1, result.size());
        assertEquals("p2", result.get(0).getProjectId());
    }

    @Test
    void execute_manager_requestedHospitalIsIgnored() {
        // D-047 第 2 點：其他角色的範圍一律是自己的院區，請求的院區參數不採用
        queryPort.summaries.add(summary("p1", "H001"));
        queryPort.summaries.add(summary("p2", "H002"));
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<ReviewProjectSummary> result = useCase.execute(actor, "H002");

        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).getProjectId());
    }

    private ReviewProjectSummary summary(String id, String hospitalId) {
        return new ReviewProjectSummary(id, hospitalId, "Name", null,
                java.util.EnumSet.of(com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType.OUTPATIENT),
                ProjectStatus.DRAFT, null, 1, false, null, null, null);
    }

    // --- Test double ---

    static class InMemoryQueryPort implements ReviewProjectQueryPort {
        final List<ReviewProjectSummary> summaries = new ArrayList<>();

        @Override
        public List<ReviewProjectSummary> findByHospitalId(String hospitalId) {
            List<ReviewProjectSummary> result = new ArrayList<>();
            for (ReviewProjectSummary s : summaries) {
                if (s.getHospitalId().equals(hospitalId)) result.add(s);
            }
            return result;
        }

        @Override
        public List<ReviewProjectSummary> findAll() {
            return new ArrayList<>(summaries);
        }

        @Override
        public Optional<ReviewProjectSummary> findSummaryById(String projectId) {
            return summaries.stream().filter(s -> s.getProjectId().equals(projectId)).findFirst();
        }
    }
}