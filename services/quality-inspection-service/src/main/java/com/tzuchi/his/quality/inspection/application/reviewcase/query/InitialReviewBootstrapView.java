package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.util.List;

/** Read model for UI-012 reviewer-facing bootstrap context. */
public class InitialReviewBootstrapView {

    private final String pageMode;
    private final boolean hasDraft;
    private final boolean hasSubmittedBase;
    private final boolean readonly;
    private final boolean blocked;
    private final String blockedReason;
    private final String scoreSheetVersionId;
    private final List<String> allowedActions;
    private final ReviewSubmissionRefView currentSubmission;
    private final ReviewSubmissionRefView editableSubmission;
    private final ReviewSubmissionRefView basedOnVersion;

    public InitialReviewBootstrapView(String pageMode, boolean hasDraft, boolean hasSubmittedBase,
                                      boolean readonly, boolean blocked, String blockedReason,
                                      String scoreSheetVersionId, List<String> allowedActions,
                                      ReviewSubmissionRefView currentSubmission,
                                      ReviewSubmissionRefView editableSubmission,
                                      ReviewSubmissionRefView basedOnVersion) {
        this.pageMode = pageMode;
        this.hasDraft = hasDraft;
        this.hasSubmittedBase = hasSubmittedBase;
        this.readonly = readonly;
        this.blocked = blocked;
        this.blockedReason = blockedReason;
        this.scoreSheetVersionId = scoreSheetVersionId;
        this.allowedActions = allowedActions;
        this.currentSubmission = currentSubmission;
        this.editableSubmission = editableSubmission;
        this.basedOnVersion = basedOnVersion;
    }

    public String getPageMode() { return pageMode; }
    public boolean isHasDraft() { return hasDraft; }
    public boolean isHasSubmittedBase() { return hasSubmittedBase; }
    public boolean isReadonly() { return readonly; }
    public boolean isBlocked() { return blocked; }
    public String getBlockedReason() { return blockedReason; }
    public String getScoreSheetVersionId() { return scoreSheetVersionId; }
    public List<String> getAllowedActions() { return allowedActions; }
    public ReviewSubmissionRefView getCurrentSubmission() { return currentSubmission; }
    public ReviewSubmissionRefView getEditableSubmission() { return editableSubmission; }
    public ReviewSubmissionRefView getBasedOnVersion() { return basedOnVersion; }
}
