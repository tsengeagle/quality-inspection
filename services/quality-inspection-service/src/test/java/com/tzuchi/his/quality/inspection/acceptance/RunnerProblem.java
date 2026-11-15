package com.tzuchi.his.quality.inspection.acceptance;

/** 範例無法執行：找不到 use case、輸入名稱對不上、變數未定義等。一律算範例未通過。 */
public class RunnerProblem extends RuntimeException {
    public RunnerProblem(String message) {
        super(message);
    }
}
