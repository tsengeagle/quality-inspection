package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetDraftCreatedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CreateScoreSheetDraftUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private CreateScoreSheetDraftUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new CreateScoreSheetDraftUseCase(repository, resolver, eventPublisher);
    }

    @Test
    void execute_hm_shouldCreateDraft() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        String id = useCase.execute(actor, "project1");

        assertNotNull(id);
        assertEquals(1, repository.size());
    }

    @Test
    void execute_shouldPublishEvent() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "project1");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ScoreSheetDraftCreatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(RoleDeniedException.class, () -> useCase.execute(actor, "project1"));
    }

    @Test
    void execute_wrongHospital_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "project1"));
    }
}
