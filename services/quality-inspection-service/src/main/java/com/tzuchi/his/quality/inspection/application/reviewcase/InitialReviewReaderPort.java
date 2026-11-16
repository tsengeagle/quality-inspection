package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.time.Instant;

/**
 * D-055 第 5 點：初審讀取者紀錄（只增不改，同一人同一案一筆），以及讀取與指派互斥所需的案件版本檢查。
 * 呼叫端必須在可寫入的交易中；紀錄在交易 commit 時才生效，commit 失敗即整筆讀取失敗（fail-closed）。
 */
public interface InitialReviewReaderPort {

    /**
     * 在判斷歷史身分之前呼叫：把案件讀入本交易，之後的 {@link #record} 以這時的版本做檢查。
     * 判斷身分到 commit 之間若有指派或改派先 commit，本交易 commit 時會以版本衝突失敗。
     */
    void pinCaseVersion(String caseId);

    boolean hasRead(String caseId, String readerId);

    /** 第一次讀取時新增紀錄，並強制遞增案件版本（不推進 updatedAt）；已有紀錄時不再寫入。 */
    void record(String caseId, String readerId, Instant readAt);
}
