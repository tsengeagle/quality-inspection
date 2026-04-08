package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetScoreSheetVersionUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private GetScoreSheetVersionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new GetScoreSheetVersionUseCase(repository, resolver);
    }

    @Test
    void execute_hm_shouldReturnVersion() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ScoreSheetVersionInfo info = useCase.execute(actor, "ss1");

        assertEquals(1, info.getVersion());
        assertFalse(info.isPublished());
    }

    @Test
    void execute_sa_shouldReturnVersion() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ScoreSheetVersionInfo info = useCase.execute(actor, "ss1");

        assertEquals(1, info.getVersion());
    }

    @Test
    void execute_hm_wrongHospital_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "ss1"));
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "nonexistent"));
    }
}
