package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.ReassignRereviewerCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewerAssignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReassignRereviewerUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;
    private final RereviewerAssignmentHistoryPort historyPort;
    private final InitialReviewReaderPort readerPort;
    private final Clock clock;

    public ReassignRereviewerUseCase(ReviewCaseRepository caseRepository,
            DomainEventPublisher eventPublisher,
            RereviewerAssignmentHistoryPort historyPort,
            InitialReviewReaderPort readerPort,
            Clock clock) {
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
        this.historyPort = historyPort;
        this.readerPort = readerPort;
        this.clock = clock;
    }

    @Transactional
    public void execute(ActorContext actor, ReassignRereviewerCommand cmd) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        // D-055 第 5 點：讀過本案完整初審的人不得受派（在變更案件之前檢查，拒絕時案件不變）。
        if (readerPort.hasRead(reviewCase.getCaseId(), cmd.getRereviewerId())) {
            throw new DomainRuleViolationException("REREVIEWER_HAS_READ_INITIAL",
                    "The rereviewer has already read this case's full initial review (D-055)");
        }
        reviewCase.reassignRereviewer(cmd.getRereviewerId(), cmd.getDueAt());
        caseRepository.save(reviewCase);
        // D-055 第 1、4 點：與指派變更同一個交易寫入歷史；任何一方失敗整筆回滾。
        historyPort.append(new RereviewerAssignmentRecord(reviewCase.getCaseId(), cmd.getRereviewerId(),
                Instant.now(clock), actor.getUserId(), RereviewerAssignmentRecord.Kind.REASSIGN));
        eventPublisher.publish(new RereviewerAssignedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"rereviewerId\":\"%s\",\"reassignment\":true}", cmd.getRereviewerId())));
    }
}
