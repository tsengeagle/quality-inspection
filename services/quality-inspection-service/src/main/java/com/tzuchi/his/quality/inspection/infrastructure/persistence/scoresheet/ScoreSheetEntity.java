package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "score_sheet")
public class ScoreSheetEntity {

    @Id
    @Column(name = "score_sheet_id")
    private String scoreSheetId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScoreSheetStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "score_sheet_id")
    @OrderColumn(name = "section_order")
    private List<ScoreSheetSectionEntity> sections = new ArrayList<>();

    protected ScoreSheetEntity() {}

    public String getScoreSheetId() { return scoreSheetId; }
    public void setScoreSheetId(String scoreSheetId) { this.scoreSheetId = scoreSheetId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public ScoreSheetStatus getStatus() { return status; }
    public void setStatus(ScoreSheetStatus status) { this.status = status; }

    public List<ScoreSheetSectionEntity> getSections() { return sections; }
    public void setSections(List<ScoreSheetSectionEntity> sections) { this.sections = sections; }
}
