package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
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

        assertEquals(1, info.getVersionNo());
        assertFalse(info.isPublished());
        assertNull(info.getScoreSheetVersionId(), "未發布時版本 id 是 null（D-050 第 1 點）");
    }

    @Test
    void execute_sa_shouldReturnVersion() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ScoreSheetVersionInfo info = useCase.execute(actor, "ss1");

        assertEquals(1, info.getVersionNo());
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
    void execute_nonManager_unknownId_shouldBeDeniedBeforeLoading() {
        // D-024：角色 → 載入 → 院區。無權的角色不應從錯誤代碼得知評分表是否存在
        ActorContext actor = new ActorContext("u1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        RoleDeniedException e = assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor, "nonexistent"));
        assertEquals("ROLE_DENIED", e.getErrorCode());
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "nonexistent"));
    }

    @Test
    void execute_auditorOfTheSameHospital_reads_D050() {
        repository.save(ScoreSheetDefinition.createDraft("ss1", "p1"));
        ActorContext auditor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H001"));

        assertEquals(1, useCase.execute(auditor, "ss1").getVersionNo());
    }

    @Test
    void execute_published_returnsTheVersionIdProducedAtPublish() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection section =
                new com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection("sec1", "General");
        section.addItem(new com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem("i1", "C01", "Item1",
                com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType.FIXED, java.math.BigDecimal.TEN, java.math.BigDecimal.ONE));
        ss.addSection(section);
        ss.publishVersion();
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ScoreSheetVersionInfo info = useCase.execute(actor, "ss1");

        assertTrue(info.isPublished());
        assertEquals(ss.getVersionId(), info.getScoreSheetVersionId());
    }
}
