package com.tzuchi.his.quality.inspection.infrastructure.persistence.demo;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;
import com.tzuchi.his.quality.inspection.domain.sampling.RuleSnapshot;
import com.tzuchi.his.quality.inspection.domain.sampling.SamplingBatch;
import com.tzuchi.his.quality.inspection.domain.sampling.SamplingBatchRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.SelectedCaseRef;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Local-only demo data that keeps standalone mode usable against the real backend.
 * This does not change the formal API contract; it only seeds a minimal local dataset.
 */
@Component
@Profile("local")
public class LocalStandaloneDemoDataSeed implements ApplicationRunner {

    private static final String HOSPITAL_ID = "H001";
    private static final String PROJECT_ID = "PROJECT-001";
    private static final String SCORE_SHEET_ID = "SS-PROJECT-001-V1";
    private static final String BATCH_ID = "BATCH-001";

    private final ReviewProjectRepository projectRepository;
    private final SamplingBatchRepository batchRepository;
    private final ReviewCaseRepository caseRepository;

    public LocalStandaloneDemoDataSeed(ReviewProjectRepository projectRepository,
                                       SamplingBatchRepository batchRepository,
                                       ReviewCaseRepository caseRepository) {
        this.projectRepository = projectRepository;
        this.batchRepository = batchRepository;
        this.caseRepository = caseRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedProject();
        seedBatch();
        seedCases();
    }

    private void seedProject() {
        if (projectRepository.findById(PROJECT_ID).isPresent()) {
            return;
        }

        ReviewProject project = ReviewProject.reconstitute(
                PROJECT_ID,
                HOSPITAL_ID,
                "2026 Q2 門診品質抽選專案",
                "Standalone demo project for sampling execution, batch tracking, and assignment flows.",
                SCORE_SHEET_ID,
                EnumSet.of(MedicalRecordType.OUTPATIENT),
                ProjectStatus.ACTIVE,
                1,
                true,
                new BigDecimal("85.00"),
                new BigDecimal("0.50"),
                new BigDecimal("0.50")
        );

        projectRepository.save(project);
    }

    private void seedBatch() {
        if (batchRepository.findById(BATCH_ID).isPresent()) {
            return;
        }

        List<SelectedCaseRef> selectedCases = demoSelectedCases();
        SamplingBatch batch = SamplingBatch.reconstitute(
                BATCH_ID,
                PROJECT_ID,
                ExecutionMode.MANUAL,
                new RuleSnapshot(PROJECT_ID + "-v1", 1, SCORE_SHEET_ID, "MANUAL", null),
                BatchStatus.COMPLETED,
                Instant.parse("2026-04-18T01:00:00Z"),
                Instant.parse("2026-04-18T01:05:00Z"),
                selectedCases.size(),
                selectedCases.size(),
                0,
                selectedCases.size(),
                null,
                null,
                selectedCases
        );

        batchRepository.save(batch);
    }

    private void seedCases() {
        List<ReviewCase> cases = List.of(
                ReviewCase.createFromSampling("CASE-011", BATCH_ID, PROJECT_ID, HOSPITAL_ID, "MR-CASE-011", MedicalRecordType.OUTPATIENT),
                ReviewCase.createFromSampling("CASE-012", BATCH_ID, PROJECT_ID, HOSPITAL_ID, "MR-CASE-012", MedicalRecordType.OUTPATIENT),
                ReviewCase.createFromSampling("CASE-013", BATCH_ID, PROJECT_ID, HOSPITAL_ID, "MR-CASE-013", MedicalRecordType.OUTPATIENT)
        );

        for (ReviewCase reviewCase : cases) {
            if (caseRepository.findById(reviewCase.getCaseId()).isEmpty()) {
                caseRepository.save(reviewCase);
            }
        }
    }

    private List<SelectedCaseRef> demoSelectedCases() {
        return List.of(
                new SelectedCaseRef("MR-CASE-011", HOSPITAL_ID, MedicalRecordType.OUTPATIENT),
                new SelectedCaseRef("MR-CASE-012", HOSPITAL_ID, MedicalRecordType.OUTPATIENT),
                new SelectedCaseRef("MR-CASE-013", HOSPITAL_ID, MedicalRecordType.OUTPATIENT)
        );
    }
}
