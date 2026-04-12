package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DashboardQueryServiceTest {

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
        DashboardQueryService service = new DashboardQueryService(null);
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        DashboardQueryCriteria criteria = new DashboardQueryCriteria(null, null, null, null);

        assertThrows(RoleDeniedException.class, () -> service.getSummary(actor, criteria));
    }
}
