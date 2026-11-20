package com.tzuchi.his.quality.inspection.acceptance;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 時鐘、埠的替身、事件擷取本身的測試（D-065 第 5、7、11 點），不啟動應用程式。 */
class AcceptanceWorldTest {

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final Instant START = Instant.parse("2026-11-19T09:00:00Z");

    // ---------- 假的埠 ----------

    public static final class Quote {
        public String symbol;
        public int price;
    }

    public interface QuotesPort {
        List<Quote> fetch(String symbol);
        void refresh();
    }

    public interface AuditPort {
        String record(String text);
    }

    public static final class RealQuotes implements QuotesPort, AuditPort {
        int fetched;

        @Override public List<Quote> fetch(String symbol) {
            fetched++;
            return Collections.emptyList();
        }
        @Override public void refresh() {
            throw new IllegalStateException("real refresh");
        }
        @Override public String record(String text) {
            return "recorded " + text;
        }
    }

    private static AcceptancePorts ports() {
        return new AcceptancePorts(new HashSet<>(Arrays.asList("Quotes", "NotImplemented")));
    }

    // ---------- 埠 ----------

    @Test
    void beanWithoutADeclaredPortIsLeftAlone() {
        Object bean = new Object();
        assertSame(bean, ports().postProcessAfterInitialization(bean, "x"));
        AuditPort audit = text -> text;
        assertSame(audit, ports().postProcessAfterInitialization(audit, "audit"));
    }

    @Test
    void declaredPortDelegatesToTheRealBeanOutsideAnExample() {
        AcceptancePorts ports = ports();
        RealQuotes real = new RealQuotes();
        QuotesPort wrapped = (QuotesPort) ports.postProcessAfterInitialization(real, "quotes");
        assertTrue(wrapped.fetch("A").isEmpty());
        assertEquals(1, real.fetched);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, wrapped::refresh);
        assertEquals("real refresh", thrown.getMessage());
    }

    @Test
    void duringAnExampleThePortReturnsOnlyWhatWasConfigured() {
        AcceptancePorts ports = ports();
        RealQuotes real = new RealQuotes();
        QuotesPort wrapped = (QuotesPort) ports.postProcessAfterInitialization(real, "quotes");

        ports.activate();
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("symbol", "A");
        row.put("price", 3);
        ports.portReturns("Quotes", "fetch", Collections.singletonList(row));
        List<Quote> quotes = wrapped.fetch("A");
        assertEquals(1, quotes.size());
        assertEquals("A", quotes.get(0).symbol);
        assertEquals(3, quotes.get(0).price);
        assertEquals(0, real.fetched);
        assertTrue(ports.takeUnconfiguredCalls().isEmpty());

        // 同一個 bean 的另一個介面沒有宣告成埠：照常交給原本的實作
        assertEquals("recorded x", ((AuditPort) wrapped).record("x"));

        ports.deactivate();
        assertTrue(wrapped.fetch("A").isEmpty());
        assertEquals(1, real.fetched);
    }

    @Test
    void reachingAnUnconfiguredOperationFailsAndIsRecorded() {
        AcceptancePorts ports = ports();
        QuotesPort wrapped = (QuotesPort) ports.postProcessAfterInitialization(new RealQuotes(), "quotes");
        ports.activate();
        AssertionError error = assertThrows(AssertionError.class, () -> wrapped.fetch("A"));
        assertTrue(error.getMessage().contains("Quotes.fetch"));
        assertEquals(Collections.singletonList("Quotes.fetch"), ports.takeUnconfiguredCalls());
        assertTrue(ports.takeUnconfiguredCalls().isEmpty());
    }

    @Test
    void configurationDoesNotCarryOverToTheNextExample() {
        AcceptancePorts ports = ports();
        QuotesPort wrapped = (QuotesPort) ports.postProcessAfterInitialization(new RealQuotes(), "quotes");
        ports.activate();
        ports.portReturns("Quotes", "fetch", Collections.emptyList());
        ports.deactivate();
        ports.activate();
        assertThrows(AssertionError.class, () -> wrapped.fetch("A"));
    }

    @Test
    void voidOperationCanBeConfigured() {
        AcceptancePorts ports = ports();
        QuotesPort wrapped = (QuotesPort) ports.postProcessAfterInitialization(new RealQuotes(), "quotes");
        ports.activate();
        ports.portReturns("Quotes", "refresh", null);
        wrapped.refresh();
    }

    @Test
    void unknownPortOperationOrValueCannotBeConfigured() {
        AcceptancePorts ports = ports();
        ports.postProcessAfterInitialization(new RealQuotes(), "quotes");
        ports.activate();
        assertTrue(assertThrows(RunnerProblem.class, () -> ports.portReturns("Audit", "record", "x"))
                .getMessage().contains("沒有在任何 model.yaml 的 ports 宣告"));
        assertTrue(assertThrows(RunnerProblem.class, () -> ports.portReturns("NotImplemented", "x", "x"))
                .getMessage().contains("沒有 bean 實作介面 NotImplementedPort"));
        assertTrue(assertThrows(RunnerProblem.class, () -> ports.portReturns("Quotes", "nope", "x"))
                .getMessage().contains("剛好一個叫 nope 的方法"));
        assertTrue(assertThrows(RunnerProblem.class, () -> ports.portReturns("Quotes", "fetch", "not a list"))
                .getMessage().contains("無法轉成"));
    }

    // ---------- 時鐘 ----------

    @Test
    void clockIsTheSystemClockUntilFixed() {
        AcceptanceClock clock = new AcceptanceClock();
        assertFalse(clock.isFixed());
        assertEquals(ZoneId.systemDefault(), clock.getZone());
        Instant before = Instant.now();
        Instant now = clock.instant();
        assertFalse(now.isBefore(before));
    }

    @Test
    void fixedClockOnlyMovesWithAdvance() {
        AcceptanceClock clock = new AcceptanceClock();
        clock.fix(START, TAIPEI);
        assertEquals(START, clock.instant());
        assertEquals(LocalDateTime.of(2026, 11, 19, 17, 0), LocalDateTime.now(clock));
        clock.advance(Duration.ofHours(2));
        assertEquals(START.plus(Duration.ofHours(2)), clock.instant());
        assertEquals(START.plus(Duration.ofHours(2)), clock.withZone(ZoneId.of("UTC")).instant());
        clock.release();
        assertFalse(clock.isFixed());
        assertThrows(RunnerProblem.class, () -> clock.advance(Duration.ofHours(1)));
    }

    // ---------- 事件與輸出 ----------

    static final class ThingRenamedEvent extends DomainEvent {
        private final String thingId;
        private final Instant renamedAt;

        ThingRenamedEvent(Object source, String thingId, Instant renamedAt) {
            super(source, "u-1", "User", "H-01", "Thing", thingId, "RENAMED", null);
            this.thingId = thingId;
            this.renamedAt = renamedAt;
        }

        public String getThingId() { return thingId; }
        public Instant getRenamedAt() { return renamedAt; }
    }

    private static SpringExampleWorld world() {
        return new SpringExampleWorld(new AcceptanceClock(), ports());
    }

    @Test
    void eventsAreCapturedOnlyDuringAnExample() {
        SpringExampleWorld world = world();
        world.onApplicationEvent(new ThingRenamedEvent(this, "t-0", START));
        world.begin(START, TAIPEI);
        assertEquals(TAIPEI, world.localTimeZone());
        world.onApplicationEvent(new ThingRenamedEvent(this, "t-1", START));

        List<ExampleWorld.EmittedEvent> events = world.takeEvents();
        assertEquals(1, events.size());
        assertEquals("ThingRenamed", events.get(0).getName());
        Map<String, Object> properties = events.get(0).getProperties();
        assertEquals("t-1", properties.get("thingId"));
        assertEquals("H-01", properties.get("hospitalId"));
        assertEquals("2026-11-19T09:00:00Z", properties.get("renamedAt"));
        // Spring 事件本身的屬性不是事件的內容；source 是發出事件的物件，不能拿來比對
        assertFalse(properties.containsKey("source"));
        assertFalse(properties.containsKey("timestamp"));
        assertTrue(world.takeEvents().isEmpty());

        world.end();
        assertEquals(null, world.localTimeZone());
        world.onApplicationEvent(new ThingRenamedEvent(this, "t-2", START));
        assertTrue(world.takeEvents().isEmpty());
    }

    public static final class Output {
        public final String id = "t-1";
        public final LocalDateTime dueAt = LocalDateTime.of(2026, 11, 27, 7, 59, 59);
        public final java.math.BigDecimal score = new java.math.BigDecimal("0.50");
        public final List<String> tags = Arrays.asList("a", "b");
    }

    @Test
    void treeKeepsTimesAsIsoTextAndDecimalsExact() {
        Object tree = world().tree(new Output());
        Map<?, ?> map = (Map<?, ?>) tree;
        assertEquals("t-1", map.get("id"));
        assertEquals("2026-11-27T07:59:59", map.get("dueAt"));
        assertEquals(Arrays.asList("a", "b"), map.get("tags"));
        assertTrue(ExampleRunner.same("0.5", map.get("score"), TAIPEI));
        // 不帶時區的輸出依 local_time_zone 解讀：07:59:59（台北）就是前一天 23:59:59Z
        assertTrue(ExampleRunner.same("2026-11-26T23:59:59Z", map.get("dueAt"), TAIPEI));
        assertFalse(ExampleRunner.same("2026-11-27T07:59:59Z", map.get("dueAt"), TAIPEI));
        assertEquals(null, world().tree(null));
    }
}
