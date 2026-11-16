package com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface RereviewerAssignmentHistoryJpaRepository extends JpaRepository<RereviewerAssignmentHistoryEntity, Long> {

    boolean existsByCaseIdAndRereviewerId(String caseId, String rereviewerId);

    @Query("select distinct h.caseId from RereviewerAssignmentHistoryEntity h"
            + " where h.rereviewerId = :rereviewerId and h.caseId in :caseIds")
    Set<String> findCaseIdsByRereviewerIdAndCaseIdIn(@Param("rereviewerId") String rereviewerId,
                                                     @Param("caseIds") Collection<String> caseIds);
}
