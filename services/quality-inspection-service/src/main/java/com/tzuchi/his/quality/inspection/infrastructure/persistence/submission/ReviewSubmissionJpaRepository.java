package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewSubmissionJpaRepository extends JpaRepository<ReviewSubmissionEntity, String> {

    List<ReviewSubmissionEntity> findByCaseIdAndStage(String caseId, ReviewStage stage);

    @Query("SELECT e FROM ReviewSubmissionEntity e " +
           "WHERE e.caseId = :caseId AND e.stage = :stage " +
           "ORDER BY e.revisionNo DESC")
    List<ReviewSubmissionEntity> findByCaseIdAndStageOrderByRevisionNoDesc(
            @Param("caseId") String caseId, @Param("stage") ReviewStage stage);

    List<ReviewSubmissionEntity> findByCaseId(String caseId);
}
