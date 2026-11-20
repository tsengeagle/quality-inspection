package com.tzuchi.his.quality.inspection.acceptance;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 驗收範例的時鐘（D-065 第 5 點）。
 *
 * <p>spec_version 2 的範例執行期間是固定的：起始值由範例或設定檔決定，只有 advance 會讓它前進。
 * 其他時候（包括 spec_version 1 的範例）就是系統時鐘，行為與應用程式原本的 Clock 相同。</p>
 */
public final class AcceptanceClock extends Clock {

    private volatile Instant fixed;
    private volatile ZoneId zone;

    /** 固定在 start，時區是 zone。 */
    public void fix(Instant start, ZoneId zone) {
        this.zone = zone;
        this.fixed = start;
    }

    public void advance(Duration amount) {
        Instant now = fixed;
        if (now == null) {
            throw new RunnerProblem("時鐘沒有固定，不能 advance");
        }
        fixed = now.plus(amount);
    }

    /** 回到系統時鐘。 */
    public void release() {
        fixed = null;
        zone = null;
    }

    public boolean isFixed() {
        return fixed != null;
    }

    @Override
    public ZoneId getZone() {
        ZoneId current = zone;
        return current != null ? current : ZoneId.systemDefault();
    }

    @Override
    public Instant instant() {
        Instant now = fixed;
        return now != null ? now : Instant.now();
    }

    @Override
    public Clock withZone(ZoneId other) {
        AcceptanceClock source = this;
        return new Clock() {
            @Override
            public ZoneId getZone() { return other; }

            @Override
            public Clock withZone(ZoneId another) { return source.withZone(another); }

            @Override
            public Instant instant() { return source.instant(); }
        };
    }
}
