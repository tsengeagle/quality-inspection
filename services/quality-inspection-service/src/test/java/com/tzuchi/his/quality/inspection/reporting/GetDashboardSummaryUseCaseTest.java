package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetDashboardSummaryUseCaseTest {

    @Test
    void zeroTotalCases_allRatesAreZero() {
        DashboardSummary s = new DashboardSummary(0, 0, 0, 0);
        assertEquals(0.0, s.getCompletionRate());
        assertEquals(0.0, s.getRereviewRate());
        assertEquals(0.0, s.getOverdueRate());
    }

    @Test
    void ratesComputedCorrectly() {
        DashboardSummary s = new DashboardSummary(10, 5, 3, 2);
        assertEquals(0.5, s.getCompletionRate(), 0.001);
        assertEquals(0.3, s.getRereviewRate(), 0.001);
        assertEquals(0.2, s.getOverdueRate(), 0.001);
    }

    @Test
    void reviewer_shouldThrowUnauthorized() {
        GetDashboardSummaryUseCase service = new GetDashboardSummaryUseCase(null, java.time.Clock.systemUTC());
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        assertThrows(RoleDeniedException.class, () -> service.execute(actor, null, null, null, null));
        // D-087 第 2 點：角色的檢查在 from、to 之前
        assertThrows(RoleDeniedException.class,
                () -> service.execute(actor, null, null, java.time.Instant.parse("2026-04-01T00:00:00Z"), null));
    }

    @Test
    void rangeChecks_areBadRequest() {
        // RC-DSH-01、RC-DSH-02
        GetDashboardSummaryUseCase service = new GetDashboardSummaryUseCase(null, java.time.Clock.systemUTC());
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        java.time.Instant t = java.time.Instant.parse("2026-04-01T00:00:00Z");

        assertEquals("BAD_REQUEST", assertThrows(DomainValidationException.class,
                () -> service.execute(actor, null, null, t, null)).getErrorCode());
        assertEquals("BAD_REQUEST", assertThrows(DomainValidationException.class,
                () -> service.execute(actor, null, null, null, t)).getErrorCode());
        assertEquals("BAD_REQUEST", assertThrows(DomainValidationException.class,
                () -> service.execute(actor, null, null, t, t)).getErrorCode());
        assertEquals("BAD_REQUEST", assertThrows(DomainValidationException.class,
                () -> service.execute(actor, null, null, t.plusSeconds(1), t)).getErrorCode());
    }
}
