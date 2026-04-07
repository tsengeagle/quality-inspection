package com.tzuchi.his.quality.inspection.application.common;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ActorContextScopeGuardTest {

    @Test
    void requireHospitalId_withHospitalId_shouldReturn() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertEquals("H001", actor.requireHospitalId());
    }

    @Test
    void requireHospitalId_withoutHospitalId_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                actor::requireHospitalId);
        assertTrue(ex.getMessage().contains("Hospital context required"));
    }

    @Test
    void requireHospitalScope_matching_shouldPass() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertDoesNotThrow(() -> actor.requireHospitalScope("H001"));
    }

    @Test
    void requireHospitalScope_mismatch_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> actor.requireHospitalScope("H002"));
        assertTrue(ex.getMessage().contains("Hospital scope mismatch"));
    }

    @Test
    void requireHospitalScope_noHospitalId_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> actor.requireHospitalScope("H001"));
    }
}
