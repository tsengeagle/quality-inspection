package com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess;

import javax.persistence.*;
import java.time.Instant;

/** D-055 第 5 點：初審讀取者。只增不改，同一人同一案一筆（unique）。 */
@Entity
@Table(name = "initial_review_reader",
        uniqueConstraints = @UniqueConstraint(name = "uk_initial_review_reader", columnNames = {"case_id", "reader_id"}))
public class InitialReviewReaderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reader_record_id")
    private Long readerRecordId;

    @Column(name = "case_id", nullable = false, updatable = false)
    private String caseId;

    @Column(name = "reader_id", nullable = false, updatable = false)
    private String readerId;

    @Column(name = "read_at", nullable = false, updatable = false)
    private Instant readAt;

    protected InitialReviewReaderEntity() {
    }

    public InitialReviewReaderEntity(String caseId, String readerId, Instant readAt) {
        this.caseId = caseId;
        this.readerId = readerId;
        this.readAt = readAt;
    }

    public Long getReaderRecordId() { return readerRecordId; }
    public String getCaseId() { return caseId; }
    public String getReaderId() { return readerId; }
    public Instant getReadAt() { return readAt; }
}
