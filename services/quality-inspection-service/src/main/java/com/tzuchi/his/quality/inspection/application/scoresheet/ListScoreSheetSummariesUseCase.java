package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListScoreSheetSummariesUseCase {

    private final ScoreSheetQueryPort queryPort;

    public ListScoreSheetSummariesUseCase(ScoreSheetQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<ScoreSheetSummary> execute(ActorContext actor) {
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return queryPort.findAll();
        }
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        return queryPort.findByHospitalId(actor.requireHospitalId());
    }
}
