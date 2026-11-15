package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/** 品質線與權重的草稿輸入能存進資料庫並原樣讀回，包括發布時才會被拒絕的權重。 */
@SpringBootTest
class ReviewProjectScoringPersistenceTest {

    @Autowired
    private ReviewProjectRepository repository;

    private static void assertScoring(ProjectVersion v, String threshold, String initial, String rereview) {
        if (threshold == null) {
            assertNull(v.getQualityThreshold());
        } else {
            assertEquals(0, new BigDecimal(threshold).compareTo(v.getQualityThreshold()));
        }
        assertEquals(0, new BigDecimal(initial).compareTo(v.getInitialReviewWeight()));
        assertEquals(0, new BigDecimal(rereview).compareTo(v.getRereviewWeight()));
    }

    @Test
    void createdDraftRoundTripsItsScoringInputs() {
        // 9.99 是型別 decimal(2) 允許的最大值；它在發布時才會被 D-020 拒絕
        repository.save(ReviewProject.create("p-scoring-1", "h1", "門診", null,
                EnumSet.of(MedicalRecordType.OUTPATIENT), new BigDecimal("7"), new BigDecimal("9.99"), new BigDecimal("0.30")));
        assertScoring(repository.findById("p-scoring-1").orElseThrow().getCurrentVersion(), "7", "9.99", "0.30");
    }

    @Test
    void updatedDraftRoundTripsItsScoringInputs() {
        ReviewProject project = ReviewProject.create("p-scoring-2", "h1", "門診", null,
                EnumSet.of(MedicalRecordType.OUTPATIENT), new BigDecimal("7"), new BigDecimal("0.70"), new BigDecimal("0.30"));
        repository.save(project);
        ReviewProject loaded = repository.findById("p-scoring-2").orElseThrow();
        loaded.updateDraft("門診", null, EnumSet.of(MedicalRecordType.OUTPATIENT), null, null, new BigDecimal("0.40"));
        repository.save(loaded);
        assertScoring(repository.findById("p-scoring-2").orElseThrow().getCurrentVersion(), null, "0.50", "0.40");
    }
}
