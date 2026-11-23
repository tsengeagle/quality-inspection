package com.tzuchi.his.quality.inspection.acceptance;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * spec_version 2 的範例在執行期間需要控制的環境（D-065 第 5、7、11 點）：時鐘、發出的事件、埠的替身。
 *
 * <p>執行器（{@link ExampleRunner}）只懂規格；時鐘怎麼換、事件怎麼擷取、埠怎麼替換，由實作決定。
 * spec_version 1 的範例不使用這個介面。</p>
 */
public interface ExampleWorld {

    /** 一個範例開始：時鐘固定在 clockStart，時區是 zone；清掉先前的事件與埠的設定。 */
    void begin(Instant clockStart, ZoneId zone);

    /** 一個範例結束（不論通過與否）：時鐘回到系統時間，埠回到原本的實作。 */
    void end();

    /** 時間前進。amount 一定是正的。 */
    void advance(Duration amount);

    /**
     * 設定埠的一個操作在這個範例期間回傳什麼。
     *
     * @throws RunnerProblem 找不到埠或操作、回傳值無法轉成操作的回傳型別
     */
    void portReturns(String port, String operation, Object returns);

    /** 取出上次呼叫之後發出的事件，並清空。 */
    List<EmittedEvent> takeEvents();

    /** 取出上次呼叫之後「執行到了、卻沒有設定 returns」的埠操作（{@code 埠.操作}），並清空。 */
    List<String> takeUnconfiguredPortCalls();

    /**
     * 把 use case 的輸出轉成只有對應表、清單與單一值的結構，供路徑選取與比對。
     * 時間要轉成 ISO 8601 字串；不帶時區的時間維持不帶時區，執行器不把它當成時間點（D-075 第 7 點）。
     */
    Object tree(Object value);

    /** 一個發出的事件：名稱是規格的事件名稱（類別 XEvent 的 X），properties 以屬性名稱為鍵。 */
    final class EmittedEvent {
        private final String name;
        private final Map<String, Object> properties;

        public EmittedEvent(String name, Map<String, Object> properties) {
            this.name = name;
            this.properties = properties;
        }

        public String getName() { return name; }
        public Map<String, Object> getProperties() { return properties; }

        @Override
        public String toString() { return name; }
    }
}
