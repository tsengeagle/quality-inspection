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
        assertTrue(ex.getMessage().contains("cannot access resource belonging to hospital"));
    }

    @Test
    void requireHospitalScope_noHospitalId_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> actor.requireHospitalScope("H001"));
    }

    @Test
    void requireReadScope_otherHospital_shouldThrowForNonAdmin() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H001"));
        assertDoesNotThrow(() -> actor.requireReadScope("H001"));
        assertThrows(AccessDeniedException.class, () -> actor.requireReadScope("H002"));
    }

    @Test
    void requireReadScope_systemAdmin_readsAcrossHospitals() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());
        assertDoesNotThrow(() -> actor.requireReadScope("H002"));
    }

    @Test
    void readScopeHospitalId_nonAdmin_ignoresRequestedHospital() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        assertEquals("H001", actor.readScopeHospitalId("H002"));
        assertEquals("H001", actor.readScopeHospitalId(null));
    }

    @Test
    void readScopeHospitalId_systemAdmin_usesRequestedHospital() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());
        assertEquals("H002", actor.readScopeHospitalId("H002"));
        assertNull(actor.readScopeHospitalId(null));
    }
}
