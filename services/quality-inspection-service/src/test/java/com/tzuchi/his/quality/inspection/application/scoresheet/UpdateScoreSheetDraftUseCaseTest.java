package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.ItemInput;
import com.tzuchi.his.quality.inspection.application.scoresheet.input.SectionInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
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
        useCase = new UpdateScoreSheetDraftUseCase(repository, resolver, eventPublisher, java.time.Clock.systemUTC(), () -> java.util.UUID.randomUUID().toString());
    }

    @Test
    void execute_addSection_shouldSucceed() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H001");

        ItemInput item = new ItemInput();
        item.setItemId("i1");
        item.setCode("C01");
        item.setTitle("Item1");
        item.setScoreType("FIXED");
        item.setMaxScore(BigDecimal.TEN);
        item.setRequiredRule("ALWAYS_REQUIRED");
        SectionInput section = new SectionInput();
        section.setSectionId("sec1");
        section.setTitle("General");
        section.setItems(List.of(item));

        useCase.execute(actor, "ss1", null, null, null, null, List.of(section), null);

        ScoreSheetDefinition saved = repository.store.get("ss1");
        assertEquals(1, saved.getSections().size());
        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ScoreSheetDraftUpdatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ActorContext actor = hmActor("H001");

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "nonexistent", null, null, null, null, null, null));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor, "ss1", null, null, null, null, null, null));
    }

    @Test
    void execute_wrongHospital_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H999");

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "ss1", null, null, null, null, null, null));
    }

    @Test
    void execute_nullListElement_isValidationErrorAndSavesNothing() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        java.util.List<SectionInput> sections = new java.util.ArrayList<>();
        sections.add(null);

        DomainValidationException e = assertThrows(DomainValidationException.class,
                () -> useCase.execute(hmActor("H001"), "ss1", null, null, null, null, sections, null));
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void execute_nullElementOnPublishedSheet_isStateConflictFirst_SSUPD01() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1", ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);
        ss.publishVersion();
        repository.save(ss);
        java.util.List<SectionInput> sections = new java.util.ArrayList<>();
        sections.add(null);

        com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException e = assertThrows(
                com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException.class,
                () -> useCase.execute(hmActor("H001"), "ss1", null, null, null, null, sections, null));
        assertEquals("INVALID_STATE_TRANSITION", e.getErrorCode());
    }

    @Test
    void execute_earlierPhaseErrorWinsOverNullElementInLaterPhase() {
        repository.save(ScoreSheetDefinition.createDraft("ss1", "p1"));
        java.util.List<SectionInput> sections = new java.util.ArrayList<>();
        sections.add(null);

        com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException e = assertThrows(
                com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException.class,
                () -> useCase.execute(hmActor("H001"), "ss1", java.util.Set.of("no-such-section"), null, null, null, sections, null));
        assertEquals("SECTION_NOT_FOUND", e.getErrorCode());
    }

    private ActorContext hmActor(String hospitalId) {
        return new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(hospitalId));
    }
}
