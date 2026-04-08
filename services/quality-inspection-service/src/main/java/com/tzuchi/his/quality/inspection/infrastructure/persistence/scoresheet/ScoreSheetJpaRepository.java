package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreSheetJpaRepository extends JpaRepository<ScoreSheetEntity, String> {
    List<ScoreSheetEntity> findByProjectIdIn(List<String> projectIds);
}
