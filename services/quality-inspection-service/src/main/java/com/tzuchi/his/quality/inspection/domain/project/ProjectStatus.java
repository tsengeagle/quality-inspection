package com.tzuchi.his.quality.inspection.domain.project;

/**
 * Lifecycle status of a ReviewProject.
 * DRAFT → ACTIVE → DEACTIVATED (one-way; DEACTIVATED is terminal)
 */
public enum ProjectStatus {
    DRAFT,
    ACTIVE,
    DEACTIVATED
}
