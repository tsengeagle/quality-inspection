package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.event.ScoreSheetBoundToProjectEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewProjectRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 綁定評分表版本（D-041、D-050 第 8 點；specs/review-project RP-BND-01～03、RP-BND-S1）。
 * 檢查順序：角色 → 專案存在 → 院區 → 專案版本未發布 → 評分表版本存在且屬於本專案 → 評分表版本已發布。
 */
class BindScoreSheetToProjectUseCaseTest {

    private InMemoryReviewProjectRepository repository;
    private InMemoryScoreSheetRepository scoreSheets;
    private CapturingDomainEventPublisher eventPublisher;
    private BindScoreSheetToProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewProjectRepository();
        scoreSheets = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new BindScoreSheetToProjectUseCase(repository, scoreSheets, eventPublisher, java.time.Clock.systemUTC(), () -> java.util.UUID.randomUUID().toString());
    }

    private static ActorContext manager(String hospitalId) {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(hospitalId));
    }

    private ReviewProject draftProject(String projectId) {
        ReviewProject project = ReviewProject.create(projectId, "H001", "Test", null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        return project;
    }

    /** 建立屬於 projectId 的評分表；publish 為 true 時發布並回傳版本 id，否則回傳它若發布會得到的 id。 */
    private String scoreSheet(String scoreSheetId, String projectId, boolean publish) {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(scoreSheetId, projectId);
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1", ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);
        if (publish) {
            ss.publishVersion();
        }
        scoreSheets.save(ss);
        return ScoreSheetVersionId.of(scoreSheetId, 1);
    }

    @Test
    void execute_publishedVersionOfThisProject_bindsTheVersionAndPublishesEvent_RPBNDS1() {
        draftProject("p1");
        String versionId = scoreSheet("ss-1", "p1", true);

        useCase.execute(manager("H001"), "p1", versionId);

        assertEquals(versionId, repository.store.get("p1").getScoreSheetVersionRef());
        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ScoreSheetBoundToProjectEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_rebinding_replacesThePreviousVersion_RPBNDS1() {
        draftProject("p1");
        String first = scoreSheet("ss-1", "p1", true);
        String second = scoreSheet("ss-2", "p1", true);
        useCase.execute(manager("H001"), "p1", first);

        useCase.execute(manager("H001"), "p1", second);

        assertEquals(second, repository.store.get("p1").getScoreSheetVersionRef());
    }

    @Test
    void execute_wrongRole_isRoleDeniedBeforeLoading() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        RoleDeniedException e = assertThrows(RoleDeniedException.class,
                () -> useCase.execute(admin, "no-such", "whatever"));
        assertEquals("ROLE_DENIED", e.getErrorCode());
    }

    @Test
    void execute_unknownProject_isProjectNotFound() {
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(manager("H001"), "no-such", "whatever"));
        assertEquals("PROJECT_NOT_FOUND", e.getErrorCode());
    }

    @Test
    void execute_otherHospital_isScopeMismatch() {
        draftProject("p1");
        String versionId = scoreSheet("ss-1", "p1", true);

        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> useCase.execute(manager("H999"), "p1", versionId));
        assertEquals("HOSPITAL_SCOPE_MISMATCH", e.getErrorCode());
    }

    @Test
    void execute_projectVersionAlreadyPublished_isInvalidStateBeforeLookingUpTheScoreSheet_RPBND01() {
        ReviewProject project = draftProject("p1");
        String versionId = scoreSheet("ss-1", "p1", true);
        useCase.execute(manager("H001"), "p1", versionId);
        project = repository.store.get("p1");
        project.updateDraft(project.getName(), project.getDescription(), project.getMedicalRecordTypes(),
                new BigDecimal("5"), project.getCurrentVersion().getInitialReviewWeight(),
                project.getCurrentVersion().getRereviewWeight());
        project.publishNewVersion();
        repository.save(project);

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(manager("H001"), "p1", "no-such-version"));
        assertEquals("INVALID_STATE_TRANSITION", e.getErrorCode());
        assertEquals(versionId, repository.store.get("p1").getScoreSheetVersionRef());
    }

    @Test
    void execute_unknownOrMalformedVersion_isScoreSheetNotFound_RPBND02() {
        draftProject("p1");
        for (String bad : new String[] {"no-such-version", ScoreSheetVersionId.of("no-such-sheet", 1), "ss-1"}) {
            ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                    () -> useCase.execute(manager("H001"), "p1", bad), bad);
            assertEquals("SCORE_SHEET_NOT_FOUND", e.getErrorCode());
        }
        assertNull(repository.store.get("p1").getScoreSheetVersionRef());
    }

    @Test
    void execute_versionNumberThatDoesNotExist_isScoreSheetNotFound_RPBND02() {
        draftProject("p1");
        scoreSheet("ss-1", "p1", true);

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(manager("H001"), "p1", ScoreSheetVersionId.of("ss-1", 2)));
        assertEquals("SCORE_SHEET_NOT_FOUND", e.getErrorCode());
    }

    @Test
    void execute_versionOfAnotherProject_isScoreSheetNotFound_RPBND02() {
        draftProject("p1");
        draftProject("p2");
        String otherProjectsVersion = scoreSheet("ss-2", "p2", true);

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(manager("H001"), "p1", otherProjectsVersion));
        assertEquals("SCORE_SHEET_NOT_FOUND", e.getErrorCode());
        assertNull(repository.store.get("p1").getScoreSheetVersionRef());
    }

    @Test
    void execute_unpublishedVersion_isInvalidState_RPBND03() {
        draftProject("p1");
        String draftVersion = scoreSheet("ss-1", "p1", false);

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(manager("H001"), "p1", draftVersion));
        assertEquals("INVALID_STATE_TRANSITION", e.getErrorCode());
        assertNull(repository.store.get("p1").getScoreSheetVersionRef());
        assertTrue(eventPublisher.events.isEmpty());
    }
}
