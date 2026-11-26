package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewerAssignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;

@Service
public class ReassignRereviewerUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;
    private final RereviewerAssignmentHistoryPort historyPort;
    private final InitialReviewReaderPort readerPort;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public ReassignRereviewerUseCase(ReviewCaseRepository caseRepository,
            DomainEventPublisher eventPublisher,
            RereviewerAssignmentHistoryPort historyPort,
            InitialReviewReaderPort readerPort,
            Clock clock,
                                     IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
        this.historyPort = historyPort;
        this.readerPort = readerPort;
        this.clock = clock;
    }

    @Transactional
    public void execute(ActorContext actor, String caseId, String rereviewerId, Instant dueAt) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        // D-082 第 7 點：輸入、狀態、與初審醫師相同先檢查，讀過初審最後（D-055 第 5 點）；
        // 都在變更案件之前，拒絕時案件不變。
        reviewCase.checkRereviewerReassignable(rereviewerId, dueAt);
        if (readerPort.hasRead(reviewCase.getCaseId(), rereviewerId)) {
            throw new DomainRuleViolationException("REREVIEWER_HAS_READ_INITIAL",
                    "The rereviewer has already read this case's full initial review (D-055)");
        }
        reviewCase.reassignRereviewer(rereviewerId, dueAt, now);
        caseRepository.save(reviewCase);
        // D-055 第 1、4 點：與指派變更同一個交易寫入歷史；任何一方失敗整筆回滾。
        historyPort.append(new RereviewerAssignmentRecord(reviewCase.getCaseId(), rereviewerId,
                now, actor.getUserId(), RereviewerAssignmentRecord.Kind.REASSIGN));
        eventPublisher.publish(new RereviewerAssignedEvent(
                this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(), rereviewerId, dueAt, true));
    }
}
