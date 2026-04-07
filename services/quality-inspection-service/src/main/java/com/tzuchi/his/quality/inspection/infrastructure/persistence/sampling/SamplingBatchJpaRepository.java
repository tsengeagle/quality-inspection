package com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SamplingBatchJpaRepository extends JpaRepository<SamplingBatchEntity, String> {
}
