package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreSheetJpaRepository extends JpaRepository<ScoreSheetEntity, String> {
}
