package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.CaseDetailMetadataView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CaseDetailMetadataMapper {
    private final ReviewProjectQueryPort projectQueryPort;

    public CaseDetailMetadataMapper(ReviewProjectQueryPort projectQueryPort) {
        this.projectQueryPort = projectQueryPort;
    }

    public CaseDetailMetadataView project(ReviewCaseDetailView source) {
        String projectName = source.getProjectId() == null ? null : projectQueryPort.findSummaryById(source.getProjectId())
                .filter(project -> Objects.equals(source.getHospitalId(), project.getHospitalId()))
                .map(ReviewProjectSummary::getName).orElse(null);
        return new CaseDetailMetadataView(source.getCaseId(), source.getStatus(), projectName,
                source.getHospitalId(), source.getReviewerId(), source.getRereviewerId(), source.getUpdatedAt());
    }
}
