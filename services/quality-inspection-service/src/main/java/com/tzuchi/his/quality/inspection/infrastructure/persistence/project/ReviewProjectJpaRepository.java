package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewProjectJpaRepository extends JpaRepository<ReviewProjectEntity, String> {

    List<ReviewProjectEntity> findByHospitalId(String hospitalId);
}
