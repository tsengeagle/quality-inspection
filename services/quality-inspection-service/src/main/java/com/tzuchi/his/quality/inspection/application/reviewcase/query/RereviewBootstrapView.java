package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Read model for UI-013 rereviewer-facing bootstrap context. */
public class RereviewBootstrapView {

    private final String pageMode;
    private final boolean hasDraft;
    private final boolean hasSubmittedBase;
    private final boolean closedRevisionException;
    private final boolean readonly;
    private final boolean blocked;
    private final String blockedReason;
    private final String scoreSheetVersionId;
    private final List<String> allowedActions;
    private final ReviewSubmissionRefView currentSubmission;
    private final ReviewSubmissionRefView editableSubmission;
    private final ReviewSubmissionRefView basedOnVersion;
    private final String initialSubmissionId;
    private final Integer initialSubmissionRevisionNo;
    private final String initialReviewerId;
    private final String initialReviewerName;
    private final BigDecimal initialTotalScore;
    private final Instant initialSubmittedAt;
    private final BigDecimal finalScore;

    public RereviewBootstrapView(String pageMode, boolean hasDraft, boolean hasSubmittedBase,
                                 boolean closedRevisionException, boolean readonly,
                                 boolean blocked, String blockedReason,
                                 String scoreSheetVersionId, List<String> allowedActions,
                                 ReviewSubmissionRefView currentSubmission,
                                 ReviewSubmissionRefView editableSubmission,
                                 ReviewSubmissionRefView basedOnVersion,
                                 String initialSubmissionId,
                                 Integer initialSubmissionRevisionNo,
                                 String initialReviewerId,
                                 String initialReviewerName,
                                 BigDecimal initialTotalScore,
                                 Instant initialSubmittedAt,
                                 BigDecimal finalScore) {
        this.pageMode = pageMode;
        this.hasDraft = hasDraft;
        this.hasSubmittedBase = hasSubmittedBase;
        this.closedRevisionException = closedRevisionException;
        this.readonly = readonly;
        this.blocked = blocked;
        this.blockedReason = blockedReason;
        this.scoreSheetVersionId = scoreSheetVersionId;
        this.allowedActions = allowedActions;
        this.currentSubmission = currentSubmission;
        this.editableSubmission = editableSubmission;
        this.basedOnVersion = basedOnVersion;
        this.initialSubmissionId = initialSubmissionId;
        this.initialSubmissionRevisionNo = initialSubmissionRevisionNo;
        this.initialReviewerId = initialReviewerId;
        this.initialReviewerName = initialReviewerName;
        this.initialTotalScore = initialTotalScore;
        this.initialSubmittedAt = initialSubmittedAt;
        this.finalScore = finalScore;
    }

    public String getPageMode() { return pageMode; }
    public boolean isHasDraft() { return hasDraft; }
    public boolean isHasSubmittedBase() { return hasSubmittedBase; }
    public boolean isClosedRevisionException() { return closedRevisionException; }
    public boolean isReadonly() { return readonly; }
    public boolean isBlocked() { return blocked; }
    public String getBlockedReason() { return blockedReason; }
    public String getScoreSheetVersionId() { return scoreSheetVersionId; }
    public List<String> getAllowedActions() { return allowedActions; }
    public ReviewSubmissionRefView getCurrentSubmission() { return currentSubmission; }
    public ReviewSubmissionRefView getEditableSubmission() { return editableSubmission; }
    public ReviewSubmissionRefView getBasedOnVersion() { return basedOnVersion; }
    public String getInitialSubmissionId() { return initialSubmissionId; }
    public Integer getInitialSubmissionRevisionNo() { return initialSubmissionRevisionNo; }
    public String getInitialReviewerId() { return initialReviewerId; }
    public String getInitialReviewerName() { return initialReviewerName; }
    public BigDecimal getInitialTotalScore() { return initialTotalScore; }
    public Instant getInitialSubmittedAt() { return initialSubmittedAt; }
    public BigDecimal getFinalScore() { return finalScore; }
}
