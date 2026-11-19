package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** D-057 第 1 點：案件建立時寫入 created_at（UTC），之後不得修改；V6 之前的資料為 NULL。 */
@SpringBootTest
class ReviewCaseCreatedAtTest {

    @Autowired private ReviewCaseRepository repository;
    @Autowired private ReviewCaseJpaRepository jpa;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clean() {
        jpa.deleteAll();
    }

    @Test
    void newCaseGetsCreatedAt_laterSavesKeepIt() {
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-1", MedicalRecordType.OUTPATIENT);
        repository.save(rc);
        Instant after = Instant.now();

        Instant createdAt = jpa.findById("c1").orElseThrow().getCreatedAt();
        assertThat(createdAt).isNotNull().isBetween(before, after);

        ReviewCase loaded = repository.findById("c1").orElseThrow();
        loaded.assignReviewer("doctor", LocalDateTime.of(2026, 12, 31, 23, 59), LocalDateTime.now());
        repository.save(loaded);

        assertThat(jpa.findById("c1").orElseThrow().getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void createdAtCannotBeOverwrittenThroughTheEntity() {
        repository.save(ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-1", MedicalRecordType.OUTPATIENT));
        ReviewCaseEntity e = jpa.findById("c1").orElseThrow();
        Instant original = e.getCreatedAt();

        e.setCreatedAt(Instant.parse("2000-01-01T00:00:00Z"));
        e.setReviewerId("doctor");
        jpa.saveAndFlush(e);

        // 測試沒有外層交易，每次查詢都是新的 persistence context，讀到的是資料庫裡的值
        ReviewCaseEntity reloaded = jpa.findById("c1").orElseThrow();
        assertThat(reloaded.getReviewerId()).as("同一次儲存的其他欄位有寫入").isEqualTo("doctor");
        assertThat(reloaded.getCreatedAt()).as("created_at 不可被覆寫").isEqualTo(original);
    }

    @Test
    void rowsWrittenWithoutTheColumnStayNull() {
        jdbc.update("insert into review_case (case_id, batch_id, project_id, status, rereview_required)"
                + " values ('legacy', 'b', 'p', 'CLOSED', 0)");
        assertThat(jdbc.queryForObject("select created_at from review_case where case_id = 'legacy'", java.sql.Timestamp.class))
                .isNull();
    }
}
