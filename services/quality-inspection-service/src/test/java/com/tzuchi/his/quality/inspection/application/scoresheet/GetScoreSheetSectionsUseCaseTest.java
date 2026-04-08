package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetScoreSheetSectionsUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private GetScoreSheetSectionsUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new GetScoreSheetSectionsUseCase(repository, resolver);
    }

    @Test
    void execute_hm_shouldReturnSections() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<ScoreSheetSection> result = useCase.execute(actor, "ss1");

        assertEquals(1, result.size());
        assertEquals("General", result.get(0).getTitle());
    }

    @Test
    void execute_sa_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "ss1"));
    }

    @Test
    void execute_hm_wrongHospital_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "ss1"));
    }
}
