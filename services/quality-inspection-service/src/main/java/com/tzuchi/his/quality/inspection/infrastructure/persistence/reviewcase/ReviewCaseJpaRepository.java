package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCaseJpaRepository extends JpaRepository<ReviewCaseEntity, String> {
}
