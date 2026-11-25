package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExportReportUseCaseTest {

    @Test
    void invalidReportType_shouldThrowBadRequest() {
        ExportReportUseCase service = new ExportReportUseCase(null, null, null);
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        java.time.Instant from = java.time.Instant.parse("2026-04-01T00:00:00Z");
        java.time.Instant to = java.time.Instant.parse("2026-05-01T00:00:00Z");

        // RC-RPT-01～03：都是 BAD_REQUEST，由問題側的例外丟出（D-087 第 2 點）
        assertEquals("BAD_REQUEST", assertThrows(DomainValidationException.class,
                () -> service.execute(actor, "INVALID", null, from, to, 0, 1000)).getErrorCode());
        assertEquals("BAD_REQUEST", assertThrows(DomainValidationException.class,
                () -> service.execute(actor, "REVIEW_COMPLETION", null, from, null, 0, 1000)).getErrorCode());
        assertEquals("BAD_REQUEST", assertThrows(DomainValidationException.class,
                () -> service.execute(actor, "REVIEW_COMPLETION", null, null, to, 0, 1000)).getErrorCode());
        assertEquals("BAD_REQUEST", assertThrows(DomainValidationException.class,
                () -> service.execute(actor, "REVIEW_COMPLETION", null, to, from, 0, 1000)).getErrorCode());
    }

    @Test
    void reviewer_shouldThrowUnauthorized() {
        ExportReportUseCase service = new ExportReportUseCase(null, null, null);
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        // D-087 第 2 點：角色的檢查在輸入的檢查之前
        assertThrows(RoleDeniedException.class, () -> service.execute(actor, "INVALID", null, null, null, null, null));
    }
}
