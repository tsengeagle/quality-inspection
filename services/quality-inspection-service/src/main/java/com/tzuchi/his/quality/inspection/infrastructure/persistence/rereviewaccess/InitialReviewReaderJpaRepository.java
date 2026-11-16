package com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InitialReviewReaderJpaRepository extends JpaRepository<InitialReviewReaderEntity, Long> {

    boolean existsByCaseIdAndReaderId(String caseId, String readerId);
}
