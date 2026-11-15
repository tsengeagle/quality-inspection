package com.tzuchi.his.quality.inspection.acceptance;

import java.util.Map;

/**
 * 範例執行器呼叫 use case 的唯一途徑。
 *
 * <p>執行器本身只懂規格（examples.yaml）；怎麼找到 use case、怎麼轉換輸入，由實作決定。
 * 正式的實作依命名慣例找 use case（規格名稱 X 對應 XUseCase.execute），不維護對照表。</p>
 */
public interface UseCaseInvoker {

    /**
     * 執行一支 use case。
     *
     * @param useCase 規格裡的 use case 名稱，例如 CreateReviewProject
     * @param actor   examples.yaml 的 actors 裡的一項：user、roles、hospital
     * @param input   已代入變數的輸入，鍵是規格的輸入名稱
     * @return 成功時帶回傳值；被拒絕時帶錯誤代碼
     * @throws RunnerProblem 找不到 use case、輸入名稱對不上之類，範例無法執行
     */
    Outcome invoke(String useCase, Map<String, Object> actor, Map<String, Object> input);

    /** 把回傳值轉成以屬性名稱為鍵的對應表，供 expect.output 比對與 save 取值。 */
    Map<String, Object> properties(Object value);
}
