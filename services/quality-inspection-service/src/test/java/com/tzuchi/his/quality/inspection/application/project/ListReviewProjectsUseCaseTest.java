package com.tzuchi.his.quality.inspection.application.project;

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

        List<ReviewProjectSummary> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).getProjectId());
    }

    @Test
    void execute_systemAdmin_shouldReturnAll() {
        queryPort.summaries.add(summary("p1", "H001"));
        queryPort.summaries.add(summary("p2", "H002"));
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        List<ReviewProjectSummary> result = useCase.execute(actor);

        assertEquals(2, result.size());
    }

    @Test
    void execute_noValidRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.empty());

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor));
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