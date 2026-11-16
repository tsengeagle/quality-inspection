package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
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

        ScoreSheetSectionsView result = useCase.execute(actor, "ss1");

        assertEquals("ss1", result.getScoreSheetId());
        assertEquals(1, result.getSections().size());
        assertEquals("General", result.getSections().get(0).getTitle());
    }

    @Test
    void execute_sa_readsAnyHospital_D050() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        assertEquals("ss1", useCase.execute(actor, "ss1").getScoreSheetId());
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
    void execute_auditorOfTheSameHospital_reads_D050() {
        repository.save(ScoreSheetDefinition.createDraft("ss1", "p1"));
        ActorContext auditor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H001"));

        assertEquals("ss1", useCase.execute(auditor, "ss1").getScoreSheetId());
    }

    @Test
    void execute_auditorOfAnotherHospital_isScopeMismatch() {
        repository.save(ScoreSheetDefinition.createDraft("ss1", "p1"));
        ActorContext auditor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H999"));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(auditor, "ss1"));
    }

    @Test
    void execute_reviewer_isRoleDenied() {
        repository.save(ScoreSheetDefinition.createDraft("ss1", "p1"));
        ActorContext reviewer = new ActorContext("u1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, "ss1"));
    }
}
