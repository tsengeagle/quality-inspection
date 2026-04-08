package com.tzuchi.his.quality.inspection.application.common;

/**
 * Resolves hospitalId via cross-aggregate relationships.
 * Implementations are in the infrastructure layer.
 */
public interface HospitalIdResolver {
    /**
     * Resolves the hospitalId for a given projectId.
     * @throws ResourceNotFoundException if the project does not exist
     */
    String resolveByProjectId(String projectId);
}
