package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "score_sheet_section")
public class ScoreSheetSectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private String sectionId;

    @Column(nullable = false)
    private String title;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id")
    @OrderColumn(name = "item_order")
    private List<ScoreItemEntity> items = new ArrayList<>();

    protected ScoreSheetSectionEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<ScoreItemEntity> getItems() { return items; }
    public void setItems(List<ScoreItemEntity> items) { this.items = items; }
}
