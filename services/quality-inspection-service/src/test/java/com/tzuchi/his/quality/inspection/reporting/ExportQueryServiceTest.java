package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExportQueryServiceTest {

    @Test
    void invalidReportType_shouldThrowBadRequest() {
        ExportQueryService service = new ExportQueryService(null, null, null);
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        ExportCriteria criteria = new ExportCriteria("INVALID", null, null, null);

        assertThrows(BadRequestException.class, () -> service.export(actor, criteria));
    }

    @Test
    void reviewer_shouldThrowUnauthorized() {
        ExportQueryService service = new ExportQueryService(null, null, null);
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        ExportCriteria criteria = new ExportCriteria("REVIEW_COMPLETION", null, null, null);

        assertThrows(RoleDeniedException.class, () -> service.export(actor, criteria));
    }
}
