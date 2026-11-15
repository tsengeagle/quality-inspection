package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetReviewProjectDetailUseCaseTest {

    private InMemoryQueryPort queryPort;
    private GetReviewProjectDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryQueryPort();
        useCase = new GetReviewProjectDetailUseCase(queryPort);
    }

    @Test
    void execute_hospitalManager_ownHospital_shouldReturn() {
        queryPort.summaries.put("p1", summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ReviewProjectSummary result = useCase.execute(actor, "p1");

        assertEquals("p1", result.getProjectId());
    }

    @Test
    void execute_systemAdmin_shouldReturnAny() {
        queryPort.summaries.put("p1", summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ReviewProjectSummary result = useCase.execute(actor, "p1");

        assertEquals("p1", result.getProjectId());
    }

    @Test
    void execute_hospitalManager_crossHospital_shouldThrow() {
        queryPort.summaries.put("p1", summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such"));
    }

    @Test
    void execute_noValidRole_shouldThrow() {
        // REVIEWER 的讀取範圍未裁決（Q-12），目前不在可讀的角色裡
        ActorContext actor = new ActorContext("u1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        RoleDeniedException e = assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor, "p1"));
        assertEquals("ROLE_DENIED", e.getErrorCode());
    }

    @Test
    void execute_auditor_ownHospital_shouldReturn() {
        // D-043：AUDITOR 可以讀專案詳情
        queryPort.summaries.put("p1", summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H001"));

        assertEquals("p1", useCase.execute(actor, "p1").getProjectId());
    }

    @Test
    void execute_auditor_crossHospital_shouldThrow() {
        // D-042、D-047：只有 SYSTEM_ADMIN 跨院區；AUDITOR 限自己的院區
        queryPort.summaries.put("p1", summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H999"));

        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1"));
        assertEquals("HOSPITAL_SCOPE_MISMATCH", e.getErrorCode());
    }

    @Test
    void execute_returnsTheFieldsTheSpecNames() {
        queryPort.summaries.put("p1", summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ReviewProjectSummary result = useCase.execute(actor, "p1");

        // specs/review-project 的 GetReviewProjectDetail output
        assertEquals(1, result.getVersionNo());
        assertFalse(result.isVersionPublished());
        assertEquals(EnumSet.of(com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType.OUTPATIENT),
                result.getMedicalRecordTypes());
        assertEquals(0, new java.math.BigDecimal("7").compareTo(result.getQualityThreshold()));
        assertEquals(0, new java.math.BigDecimal("0.50").compareTo(result.getInitialReviewWeight()));
        assertEquals(0, new java.math.BigDecimal("0.50").compareTo(result.getRereviewWeight()));
    }

    private ReviewProjectSummary summary(String id, String hospitalId) {
        return new ReviewProjectSummary(id, hospitalId, "Name", null,
                java.util.EnumSet.of(com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType.OUTPATIENT),
                ProjectStatus.DRAFT, null, 1, false, new java.math.BigDecimal("7"),
                new java.math.BigDecimal("0.50"), new java.math.BigDecimal("0.50"));
    }

    // --- Test double ---

    static class InMemoryQueryPort implements ReviewProjectQueryPort {
        final Map<String, ReviewProjectSummary> summaries = new HashMap<>();

        @Override
        public List<ReviewProjectSummary> findByHospitalId(String hospitalId) {
            List<ReviewProjectSummary> result = new ArrayList<>();
            for (ReviewProjectSummary s : summaries.values()) {
                if (s.getHospitalId().equals(hospitalId)) result.add(s);
            }
            return result;
        }

        @Override
        public List<ReviewProjectSummary> findAll() {
            return new ArrayList<>(summaries.values());
        }

        @Override
        public Optional<ReviewProjectSummary> findSummaryById(String projectId) {
            return Optional.ofNullable(summaries.get(projectId));
        }
    }
}