package com.tzuchi.his.quality.inspection.application.reviewcase;

/**
 * D-055 第 1～3 點：只增不改的複審指派歷史，以及案件的「歷史完整」旗標。
 * 沒有修改或刪除的方法；完整性只在案件建立時決定，這裡只能讀取。
 */
public interface RereviewerAssignmentHistoryPort {

    /** 與案件的指派變更在同一個交易寫入（呼叫端必須已在交易中）。 */
    void append(RereviewerAssignmentRecord record);

    /** 這個人是否曾被指派或改派為該案的複審醫師。 */
    boolean contains(String caseId, String rereviewerId);

    /** 該案的複審指派歷史是否完整；案件不存在時視為不完整。 */
    boolean isComplete(String caseId);
}
