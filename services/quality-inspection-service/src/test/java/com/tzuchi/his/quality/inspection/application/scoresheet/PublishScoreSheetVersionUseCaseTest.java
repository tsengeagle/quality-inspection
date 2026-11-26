package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetVersionPublishedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PublishScoreSheetVersionUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private PublishScoreSheetVersionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new PublishScoreSheetVersionUseCase(repository, resolver, eventPublisher, java.time.Clock.systemUTC(), () -> java.util.UUID.randomUUID().toString());
    }

    @Test
    void execute_shouldPublish() {
        ScoreSheetDefinition ss = createDraftWithItem("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H001");

        PublishScoreSheetVersionResult result = useCase.execute(actor, "ss1");

        assertEquals(ScoreSheetStatus.PUBLISHED, repository.store.get("ss1").getStatus());
        // D-050 第 1 點：發布產生並回傳版本 id（SS-PUB-S2）
        assertEquals(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of("ss1", 1),
                result.getScoreSheetVersionId());
        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ScoreSheetVersionPublishedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ActorContext actor = hmActor("H001");
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "nonexistent"));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ScoreSheetDefinition ss = createDraftWithItem("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(RoleDeniedException.class, () -> useCase.execute(actor, "ss1"));
    }

    @Test
    void execute_wrongHospital_shouldThrow() {
        ScoreSheetDefinition ss = createDraftWithItem("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H999");

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "ss1"));
    }

    private ScoreSheetDefinition createDraftWithItem(String id, String projectId) {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(id, projectId);
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);
        return ss;
    }

    private ActorContext hmActor(String hospitalId) {
        return new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(hospitalId));
    }
}
