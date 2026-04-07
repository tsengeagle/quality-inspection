package com.tzuchi.his.quality.inspection.domain.project;

import java.util.Objects;

/**
 * Immutable version identifier for a ReviewProject.
 */
public class ProjectVersion {

    private final int versionNumber;
    private final boolean published;

    public ProjectVersion(int versionNumber, boolean published) {
        if (versionNumber < 1) {
            throw new IllegalArgumentException("Version number must be >= 1");
        }
        this.versionNumber = versionNumber;
        this.published = published;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public boolean isPublished() {
        return published;
    }

    public ProjectVersion publish() {
        return new ProjectVersion(this.versionNumber, true);
    }

    public ProjectVersion nextDraft() {
        return new ProjectVersion(this.versionNumber + 1, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectVersion that = (ProjectVersion) o;
        return versionNumber == that.versionNumber && published == that.published;
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionNumber, published);
    }

    @Override
    public String toString() {
        return "v" + versionNumber + (published ? " (published)" : " (draft)");
    }
}
