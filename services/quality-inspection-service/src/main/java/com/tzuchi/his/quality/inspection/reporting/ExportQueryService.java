package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.ExportCaseQuery;
import com.tzuchi.his.quality.inspection.application.common.ExportCaseRecord;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.RereviewerHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExportQueryService {

    private final ExportCaseQuery caseQuery;
    private final DashboardQueryService dashboardQueryService;
    private final RereviewerHistoryQuery historyQuery;

    /** D-055 第 6 點：進入這些狀態後就不可能再指派或改派複審醫師。 */
    private static final Set<CaseStatus> NO_FURTHER_REREVIEWER = EnumSet.of(
            CaseStatus.REREVIEW_IN_PROGRESS, CaseStatus.CLOSED, CaseStatus.CANCELLED);
    private static final List<String> SCORE_FIELDS = List.of("initialScore", "finalScore");

    public ExportQueryService(ExportCaseQuery caseQuery, DashboardQueryService dashboardQueryService,
                              RereviewerHistoryQuery historyQuery) {
        this.caseQuery = caseQuery;
        this.dashboardQueryService = dashboardQueryService;
        this.historyQuery = historyQuery;
    }

    /**
     * D-057：依案件建立時間過濾（from ≤ createdAt < to，兩者必填，由 controller 驗證後回 400），
     * 逐案資料列依 caseId 遞增分頁；summary 以整個過濾範圍計算，不受分頁影響，且與 totalElements 為同一個母體。
     */
    // 計數、逐案資料列與 summary 是多次查詢；SERIALIZABLE 讓整次匯出讀同一個快照（Oracle 的 SERIALIZABLE 為
    // 交易層級的一致讀取），查詢之間提交的案件不會只算進其中一部分。匯出只讀不寫，不會有序列化衝突。
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public ExportResult export(ActorContext actor, ExportCriteria criteria) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        if (!"REVIEW_COMPLETION".equals(criteria.getReportType())) {
            throw new BadRequestException("Unsupported reportType: " + criteria.getReportType());
        }
        // 防禦：時間區間缺漏時不得退回「匯出全部」（controller 已先回 400）
        Objects.requireNonNull(criteria.getFrom(), "from");
        Objects.requireNonNull(criteria.getTo(), "to");

        // D-042、D-047：院區取自登入者，請求帶的院區只有 SYSTEM_ADMIN 能用來縮小範圍
        String hospitalId = actor.readScopeHospitalId(criteria.getHospitalId());

        Page<ExportCaseRecord> page = caseQuery.findCases(hospitalId, criteria.getFrom(), criteria.getTo(),
                criteria.getPage(), criteria.getSize());
        List<ExportRow> rows = maskScores(actor, page.getContent().stream()
                .map(ExportQueryService::toRow)
                .collect(Collectors.toList()));
        DashboardSummary summary = dashboardQueryService.getSummary(actor,
                new DashboardQueryCriteria(hospitalId, null, criteria.getFrom(), criteria.getTo()));

        return new ExportResult(criteria.getReportType(), rows, summary,
                page.getPage(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static ExportRow toRow(ExportCaseRecord c) {
        return new ExportRow(c.getCaseId(), c.getProjectId(), c.getHospitalId(), c.getStatus(),
                c.getReviewerId(), c.getRereviewerId(), c.getInitialScore(), c.getFinalScore(),
                c.getCreatedAt(), c.getClosedAt());
    }

    /**
     * D-055 第 6 點：匯出不記錄讀取者，改為遮罩逐案分數。下列任一成立即遮罩：
     * SYSTEM_ADMIN（metadata／aggregated only）；案件之後仍可能指派複審；匯出者是現任或歷史複審醫師；
     * 該案歷史不完整（無法證明匯出者不是歷史複審醫師）。
     */
    private List<ExportRow> maskScores(ActorContext actor, List<ExportRow> rows) {
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return rows.stream().map(ExportQueryService::masked).collect(Collectors.toList());
        }
        // 只有「狀態已不可能再指派、且匯出者不是現任複審醫師」的案件需要查歷史；其他一律遮罩，不必查。
        List<String> candidates = rows.stream()
                .filter(r -> !maskedWithoutHistory(actor, r))
                .map(ExportRow::getCaseId)
                .collect(Collectors.toList());
        Set<String> historicalCases = candidates.isEmpty() ? Set.of()
                : historyQuery.findCaseIdsAssignedTo(actor.getUserId(), candidates);
        Set<String> incompleteCases = candidates.isEmpty() ? Set.of()
                : historyQuery.findHistoryIncompleteCaseIds(candidates);
        return rows.stream()
                .map(r -> maskedWithoutHistory(actor, r) || historicalCases.contains(r.getCaseId())
                        || incompleteCases.contains(r.getCaseId()) ? masked(r) : r)
                .collect(Collectors.toList());
    }

    private static boolean maskedWithoutHistory(ActorContext actor, ExportRow r) {
        return !NO_FURTHER_REREVIEWER.contains(CaseStatus.valueOf(r.getStatus()))
                || Objects.equals(actor.getUserId(), r.getRereviewerId());
    }

    private static ExportRow masked(ExportRow r) {
        return new ExportRow(r.getCaseId(), r.getProjectId(), r.getHospitalId(), r.getStatus(),
                r.getReviewerId(), r.getRereviewerId(), null, null, r.getCreatedAt(), r.getClosedAt(), SCORE_FIELDS);
    }
}
