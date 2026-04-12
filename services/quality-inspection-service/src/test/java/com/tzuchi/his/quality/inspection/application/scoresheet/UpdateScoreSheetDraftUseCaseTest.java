package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetDraftUpdatedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateScoreSheetDraftUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private UpdateScoreSheetDraftUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new UpdateScoreSheetDraftUseCase(repository, resolver, eventPublisher);
    }

    @Test
    void execute_addSection_shouldSucceed() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H001");

        UpdateDraftCommand cmd = new UpdateDraftCommand(
                List.of(new UpdateDraftCommand.AddSectionCommand("sec1", "General",
                        List.of(new UpdateDraftCommand.AddItemCommand("sec1", "i1", "C01", "Item1",
                                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
                                RequiredRule.ALWAYS_REQUIRED, null)))),
                null, null, null, null, null);

        useCase.execute(actor, "ss1", cmd);

        ScoreSheetDefinition saved = repository.store.get("ss1");
        assertEquals(1, saved.getSections().size());
        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ScoreSheetDraftUpdatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ActorContext actor = hmActor("H001");
        UpdateDraftCommand cmd = new UpdateDraftCommand(null, null, null, null, null, null);

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "nonexistent", cmd));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));
        UpdateDraftCommand cmd = new UpdateDraftCommand(null, null, null, null, null, null);

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor, "ss1", cmd));
    }

    @Test
    void execute_wrongHospital_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H999");
        UpdateDraftCommand cmd = new UpdateDraftCommand(null, null, null, null, null, null);

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "ss1", cmd));
    }

    private ActorContext hmActor(String hospitalId) {
        return new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(hospitalId));
    }
}
