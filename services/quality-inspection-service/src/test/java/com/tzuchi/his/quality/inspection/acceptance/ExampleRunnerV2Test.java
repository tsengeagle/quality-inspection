package com.tzuchi.his.quality.inspection.acceptance;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 執行器對 spec_version 2（D-065）的測試：該通過的通過、該失敗的失敗，而且失敗訊息指出原因。
 *
 * <p>用假的 use case 與假的環境，不依賴應用程式，所以這裡的失敗只可能是執行器的錯。</p>
 */
class ExampleRunnerV2Test {

    private static final Set<String> SPECIFIED = new HashSet<>(Arrays.asList(
            "CreateThing", "GetThing", "ListThings", "RenameThing", "QuoteThing", "CreateOwner"));

    /** 假的環境：時鐘、事件與埠都在記憶體裡。 */
    static final class FakeWorld implements ExampleWorld {
        Instant now;
        ZoneId zone;
        int begun;
        int ended;
        final List<EmittedEvent> events = new ArrayList<>();
        final Map<String, Object> ports = new LinkedHashMap<>();
        final List<String> unconfigured = new ArrayList<>();

        @Override public void begin(Instant clockStart, ZoneId zone) {
            begun++;
            now = clockStart;
            this.zone = zone;
            events.clear();
            ports.clear();
            unconfigured.clear();
        }
        @Override public void end() { ended++; }
        @Override public void advance(Duration amount) { now = now.plus(amount); }
        @Override public void portReturns(String port, String operation, Object returns) {
            if (!"Quotes".equals(port)) {
                throw new RunnerProblem("找不到埠 " + port);
            }
            ports.put(port + "." + operation, returns);
        }
        @Override public List<EmittedEvent> takeEvents() {
            List<EmittedEvent> out = new ArrayList<>(events);
            events.clear();
            return out;
        }
        @Override public List<String> takeUnconfiguredPortCalls() {
            List<String> out = new ArrayList<>(unconfigured);
            unconfigured.clear();
            return out;
        }
        @Override public Object tree(Object value) { return value; }

        void emit(String name, Object... pairs) {
            Map<String, Object> properties = new LinkedHashMap<>();
            for (int i = 0; i < pairs.length; i += 2) {
                properties.put((String) pairs[i], pairs[i + 1]);
            }
            events.add(new EmittedEvent(name, properties));
        }
    }

    /**
     * 假的 use case。
     * CreateThing 發出 ThingCreated；RenameThing 名稱空白時拒絕，名稱是 leak 時拒絕卻發出事件（程式的錯）；
     * GetThing 對不是擁有者的操作者遮掉 secret 與 parts 的 note；QuoteThing 讀埠 Quotes.fetch。
     */
    static final class FakeInvoker implements UseCaseInvoker {
        final FakeWorld world;
        final Map<String, Map<String, Object>> things = new LinkedHashMap<>();
        final List<String> calls = new ArrayList<>();

        FakeInvoker(FakeWorld world) { this.world = world; }

        @Override
        public Outcome invoke(String useCase, Map<String, Object> actor, Map<String, Object> input) {
            calls.add(useCase + " as " + actor.get("user"));
            boolean owner = "u-1".equals(actor.get("user"));
            switch (useCase) {
                case "CreateThing": {
                    if (!owner) {
                        return Outcome.rejected("ROLE_DENIED", "not the owner");
                    }
                    String id = "t-" + (things.size() + 1);
                    Map<String, Object> thing = new LinkedHashMap<>();
                    thing.put("id", id);
                    thing.put("name", input.get("name"));
                    thing.put("dueAt", input.get("dueAt"));
                    // 不帶時區的當地時間，像應用程式的 LocalDateTime
                    thing.put("createdAt", LocalDateTime.ofInstant(world.now, world.zone).toString());
                    thing.put("secret", "s3cret");
                    thing.put("parts", Arrays.asList(part("A", 1, "first"), part("B", 2, "second")));
                    things.put(id, thing);
                    world.emit("ThingCreated", "id", id, "name", input.get("name"));
                    world.emit("ThingIndexed", "id", id);
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("id", id);
                    out.put("status", "NEW");
                    return Outcome.success(out);
                }
                case "GetThing": {
                    Map<String, Object> thing = things.get(String.valueOf(input.get("id")));
                    if (thing == null) {
                        return Outcome.rejected("NOT_FOUND", "none");
                    }
                    if ("u-9".equals(actor.get("user"))) {
                        return Outcome.rejected("ACCESS_DENIED", "stranger");
                    }
                    return Outcome.success(owner ? thing : masked(thing));
                }
                case "ListThings": {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("content", new ArrayList<>(things.values()));
                    return Outcome.success(out);
                }
                case "RenameThing": {
                    Object name = input.get("name");
                    if ("leak".equals(name)) {
                        world.emit("ThingRenamed", "id", input.get("id"));
                        return Outcome.rejected("VALIDATION_ERROR", "leaked an event");
                    }
                    if (name == null || String.valueOf(name).trim().isEmpty()) {
                        return Outcome.rejected("VALIDATION_ERROR", "blank");
                    }
                    things.get(String.valueOf(input.get("id"))).put("name", name);
                    world.emit("ThingRenamed", "id", input.get("id"), "name", name);
                    return Outcome.success(null);
                }
                case "QuoteThing": {
                    if (!world.ports.containsKey("Quotes.fetch")) {
                        world.unconfigured.add("Quotes.fetch");
                        return Outcome.rejected("UPSTREAM_ERROR", "swallowed");
                    }
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("quotes", world.ports.get("Quotes.fetch"));
                    return Outcome.success(out);
                }
                default:
                    throw new RunnerProblem("找不到 use case " + useCase);
            }
        }

        private static Map<String, Object> part(String kind, int size, String note) {
            Map<String, Object> part = new LinkedHashMap<>();
            part.put("kind", kind);
            part.put("size", size);
            part.put("note", note);
            return part;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> masked(Map<String, Object> thing) {
            Map<String, Object> out = new LinkedHashMap<>(thing);
            out.put("secret", null);
            List<Object> parts = new ArrayList<>();
            for (Map<String, Object> part : (List<Map<String, Object>>) thing.get("parts")) {
                Map<String, Object> copy = new LinkedHashMap<>(part);
                copy.remove("note");
                parts.add(copy);
            }
            out.put("parts", parts);
            return out;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> properties(Object value) {
            return (Map<String, Object>) value;
        }
    }

    private static final String HEADER = String.join("\n",
            "spec_version: 2",
            "kind: examples",
            "actors:",
            "  owner:    { user: u-1, roles: [OWNER], hospital: H-01 }",
            "  reader:   { user: u-2, roles: [READER], hospital: H-01 }",
            "  stranger: { user: u-9, roles: [OWNER], hospital: H-02 }",
            "fixtures:",
            "  thing:",
            "    steps:",
            "      - run: CreateThing",
            "        as: owner",
            "        input: { name: apple }",
            "        save: { t: id }",
            "  thing_in_NEW:",
            "    steps:",
            "      - use: thing",
            "  second_thing:",
            "    steps:",
            "      - use: other.thing_of_someone",
            "expectations:",
            "  full:    { result: SUCCESS, output: { secret: s3cret } }",
            "  masked:  { result: SUCCESS, absent: [secret, \"parts[*].note\"] }",
            "  denied:  { result: REJECTED, code: ACCESS_DENIED }",
            "examples:",
            "");

    /** 另一個規格目錄（版本 1）：它的 fixture 用它自己的操作者，owner 在那裡是 u-1。 */
    private static final String OTHER = String.join("\n",
            "spec_version: 1",
            "kind: examples",
            "actors:",
            "  someone: { user: u-1, roles: [OWNER], hospital: H-01 }",
            "fixtures:",
            "  thing_of_someone:",
            "    steps:",
            "      - run: CreateThing",
            "        as: someone",
            "        input: { name: pear }",
            "        save: { o: id }",
            "examples: []",
            "");

    private static final String PROFILE = String.join("\n",
            "kind: spec-profile",
            "profile_version: 2",
            "clock_start: \"2026-11-19T17:00:00+08:00\"",
            "local_time_zone: Asia/Taipei",
            "");

    private FakeWorld world;
    private FakeInvoker invoker;

    private ExampleRunner runner(String... exampleLines) {
        return runnerWithProfile(PROFILE, exampleLines);
    }

    private ExampleRunner runnerWithProfile(String profile, String... exampleLines) {
        world = new FakeWorld();
        invoker = new FakeInvoker(world);
        Map<String, Map<String, Object>> documents = new LinkedHashMap<>();
        documents.put("home", new Yaml().load(HEADER + String.join("\n", exampleLines) + "\n"));
        documents.put("other", new Yaml().load(OTHER));
        Map<String, Object> profileDoc = new Yaml().load(profile);
        return new ExampleRunner("home", documents, profileDoc, SPECIFIED, invoker, world);
    }

    private static String failure(ExampleRunner runner, String name) {
        AssertionError error = assertThrows(AssertionError.class, () -> runner.run(name));
        return error.getMessage();
    }

    private static String failure(ExampleRunner runner) {
        return failure(runner, "e");
    }

    // --- then 比對 when 的輸出 ---

    @Test
    void thenComparesTheOutputOfWhen() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: owner, input: { name: apple }, save: { t: id } }",
                "    then: { result: SUCCESS, output: { id: \"${t}\", status: NEW } }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void thenOutputMismatchFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: owner, input: { name: apple } }",
                "    then: { result: SUCCESS, output: { status: OLD } }");
        assertTrue(failure(r).contains("status 預期 OLD，實際 NEW"));
    }

    // --- 路徑 ---

    @Test
    void filterPathSelectsMatchingItems() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then:",
                "      result: SUCCESS",
                "      output:",
                "        \"parts[kind=B]\": { size: 2, note: second }",
                "        \"parts[kind=A,size=1]\": { note: first }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void filterValueCanBeAVariable() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS, output: { \"content[id=${t}]\": { name: apple } } }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void filterThatSelectsNothingFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS, output: { \"parts[kind=Z]\": { size: 2 } } }");
        assertTrue(failure(r).contains("parts[kind=Z] 選不到任何一筆"));
    }

    @Test
    void filterMatchWithWrongValueFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS, output: { \"parts[kind=B]\": { size: 3 } } }");
        assertTrue(failure(r).contains("parts[kind=B] 預期"));
    }

    @Test
    void wildcardPathChecksEveryItem() {
        ExampleRunner pass = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS, output: { \"content[*].name\": apple } }");
        assertDoesNotThrow(() -> pass.run("e"));

        ExampleRunner fail = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS, output: { \"parts[*].size\": 1 } }");
        assertTrue(failure(fail).contains("parts[*].size 預期 1，實際 2"));
    }

    @Test
    void listWrittenDirectlyKeepsVersionOneSemantics() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS, output: { parts: [{ kind: B }] } }");
        assertTrue(failure(r).contains("parts 預期"));
    }

    @Test
    void indexPathIsOnlyForSave() {
        ExampleRunner save = runner(
                "  - name: e",
                "    given:",
                "      - use: thing",
                "      - { run: ListThings, as: owner, save: { first: \"content[0].id\" } }",
                "    when: { run: GetThing, as: owner, input: { id: \"${first}\" } }",
                "    then: { result: SUCCESS, output: { name: apple } }");
        assertDoesNotThrow(() -> save.run("e"));

        ExampleRunner output = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS, output: { \"content[0].name\": apple } }");
        assertTrue(failure(output).contains("a[0] 只能用在 save"));
    }

    @Test
    void saveOfAnEmptyListFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given:",
                "      - { run: ListThings, as: owner, save: { first: \"content[0].id\" } }",
                "    when: { run: GetThing, as: owner, input: { id: \"${first}\" } }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("輸出沒有 content[0].id"));
    }

    // --- absent 與 only ---

    @Test
    void absentPassesWhenMissingOrNull() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: reader, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS, absent: [secret, \"parts[*].note\", noSuchField] }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void absentFailsWhenTheValueIsThere() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS, absent: [\"parts[*].note\"] }");
        assertTrue(failure(r).contains("parts[*].note 應該不存在或為 null"));
    }

    @Test
    void absentOnAListThatSelectsNothingFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: reader, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS, absent: [\"parts[kind=Z].note\"] }");
        assertTrue(failure(r).contains("選不到任何一筆"));
    }

    @Test
    void onlyAllowsListedNonNullProperties() {
        ExampleRunner pass = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: reader, input: { id: \"${t}\" } }",
                "    then:",
                "      result: SUCCESS",
                "      only:",
                "        \".\": [id, name, dueAt, createdAt, parts]",
                "        \"parts[*]\": [kind, size]");
        assertDoesNotThrow(() -> pass.run("e"));

        ExampleRunner fail = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then:",
                "      result: SUCCESS",
                "      only:",
                "        \".\": [id, name, dueAt, createdAt, parts]",
                "        \"parts[*]\": [kind, size]");
        String message = failure(fail);
        assertTrue(message.contains(". 有清單以外的屬性 secret"), message);
        assertTrue(message.contains("parts[*] 有清單以外的屬性 note"), message);
    }

    // --- 具名斷言與矩陣 ---

    @Test
    void matrixExpandsIntoNamedExamples() {
        ExampleRunner r = runner(
                "  - name: visibility",
                "    matrix:",
                "      columns: [actor, state, expect]",
                "      key: [actor, state]",
                "      rows:",
                "        - [owner,    NEW, full]",
                "        - [reader,   NEW, masked]",
                "        - [stranger, NEW, denied]",
                "    given: [{ use: \"thing_in_${state}\" }]",
                "    when: { run: GetThing, as: \"${actor}\", input: { id: \"${t}\" } }",
                "    then: { use_expect: \"${expect}\" }");
        assertEquals(Arrays.asList("visibility.owner.NEW", "visibility.reader.NEW", "visibility.stranger.NEW"),
                r.exampleNames());
        for (String name : r.exampleNames()) {
            assertDoesNotThrow(() -> r.run(name), name);
        }
    }

    @Test
    void matrixRowWithTheWrongExpectationFails() {
        ExampleRunner r = runner(
                "  - name: visibility",
                "    matrix:",
                "      columns: [actor, expect]",
                "      key: [actor]",
                "      rows:",
                "        - [reader, full]",
                "        - [owner, denied]",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: \"${actor}\", input: { id: \"${t}\" } }",
                "    then: { use_expect: \"${expect}\" }");
        assertTrue(failure(r, "visibility.reader").contains("secret 預期 s3cret"));
        assertTrue(failure(r, "visibility.owner").contains("預期 REJECTED ACCESS_DENIED，實際 SUCCESS"));
    }

    @Test
    void matrixKeyValueMustBeSafeForAName() {
        RunnerProblem problem = assertThrows(RunnerProblem.class, () -> runner(
                "  - name: bad",
                "    matrix: { columns: [who], key: [who], rows: [[\"a b\"]] }",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS }"));
        assertTrue(problem.getMessage().contains("只能用英數字"));
    }

    @Test
    void unknownNamedExpectationCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: ListThings, as: owner }",
                "    then: { use_expect: nope }");
        assertTrue(failure(r).contains("use_expect 引用了不存在的具名斷言 nope"));
    }

    // --- 時間 ---

    @Test
    void clockStartsAtTheProfileValueAndTimesCompareAsInstants() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                // createdAt 是不帶時區的 17:00（台北）；規格寫 UTC 的 09:00，是同一個時間點
                "    then: { result: SUCCESS, output: { createdAt: \"2026-11-19T09:00:00Z\" } }");
        assertDoesNotThrow(() -> r.run("e"));
        assertEquals(ZoneId.of("Asia/Taipei"), world.zone);
    }

    @Test
    void differentInstantFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS, output: { createdAt: \"2026-11-19T17:00:00Z\" } }");
        assertTrue(failure(r).contains("createdAt 預期"));
    }

    @Test
    void exampleClockAndAdvance() {
        ExampleRunner r = runner(
                "  - name: e",
                "    clock: { start: \"2026-01-01T00:00:00Z\" }",
                "    given:",
                "      - advance: PT1H30M",
                "      - use: thing",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS, output: { createdAt: \"2026-01-01T09:30:00+08:00\" } }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void advanceBackwardsCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ advance: \"-PT1H\" }]",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("advance 只能前進"));
    }

    @Test
    void profileWithoutClockCannotRun() {
        ExampleRunner r = runnerWithProfile("kind: spec-profile\nprofile_version: 1\n",
                "  - name: e",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("local_time_zone"));
        assertEquals(0, world.begun);
    }

    // --- 事件 ---

    @Test
    void emitsIsTheCompleteListInAnyOrder() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: owner, input: { name: apple }, save: { t: id } }",
                "    then:",
                "      result: SUCCESS",
                "      emits:",
                "        - { event: ThingIndexed, with: { id: \"${t}\" } }",
                "        - { event: ThingCreated, with: { id: \"${t}\", name: apple } }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void emitsFailsWhenAnEventIsNotListed() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: owner, input: { name: apple } }",
                "    then: { result: SUCCESS, emits: [{ event: ThingCreated }] }");
        assertTrue(failure(r).contains("then.emits：預期剛好發出"));
    }

    @Test
    void emitsFailsOnAWrongProperty() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: owner, input: { name: apple } }",
                "    then:",
                "      result: SUCCESS",
                "      emits:",
                "        - { event: ThingIndexed }",
                "        - { event: ThingCreated, with: { name: pear } }");
        assertTrue(failure(r).contains("then.emits"));
    }

    @Test
    void emitsIncludeIsPartial() {
        ExampleRunner pass = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: owner, input: { name: apple } }",
                "    then: { result: SUCCESS, emits_include: [{ event: ThingCreated, with: { name: apple } }] }");
        assertDoesNotThrow(() -> pass.run("e"));

        ExampleRunner fail = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: owner, input: { name: apple } }",
                "    then: { result: SUCCESS, emits_include: [{ event: ThingRenamed }] }");
        assertTrue(failure(fail).contains("then.emits_include"));
    }

    @Test
    void eventsFromGivenStepsAreNotCounted() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: RenameThing, as: owner, input: { id: \"${t}\", name: pear } }",
                "    then: { result: SUCCESS, emits: [{ event: ThingRenamed, with: { name: pear } }] }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void rejectionThatEmittedAnEventFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: RenameThing, as: owner, input: { id: \"${t}\", name: leak } }",
                "    then: { result: REJECTED, code: VALIDATION_ERROR }");
        assertTrue(failure(r).contains("被拒絕卻發出了事件"));
    }

    // --- 跨目錄 ---

    @Test
    void fixtureFromAnotherDirectoryUsesThatDirectorysActors() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: second_thing }]",
                "    when: { run: other.GetThing, as: owner, input: { id: \"${o}\" } }",
                "    then: { result: SUCCESS, output: { name: pear } }");
        assertDoesNotThrow(() -> r.run("e"));
        // someone 只定義在 other；qualified 的 run 只用名稱找 use case
        assertEquals(Arrays.asList("CreateThing as u-1", "GetThing as u-1"), invoker.calls);
    }

    @Test
    void unknownFixtureCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: other.nope }]",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("沒有 fixture other.nope"));
        assertTrue(invoker.calls.isEmpty());
    }

    // --- 埠 ---

    @Test
    void portReturnsIsUsedDuringTheExample() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given:",
                "      - port: home.Quotes.fetch",
                "        returns: [{ price: 3 }]",
                "    when: { run: QuoteThing, as: owner }",
                "    then: { result: SUCCESS, output: { quotes: [{ price: 3 }] } }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void reachingAPortWithoutReturnsFailsEvenIfTheRejectionWasExpected() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: QuoteThing, as: owner }",
                "    then: { result: REJECTED, code: UPSTREAM_ERROR }");
        assertTrue(failure(r).contains("執行到沒有設定 returns 的埠：Quotes.fetch"));
    }

    @Test
    void undeclaredPortCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ port: Nope.fetch, returns: [] }]",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("無法執行：找不到埠 Nope"));
    }

    // --- 環境的開始與結束 ---

    @Test
    void worldEndsEvenWhenTheExampleFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: reader, input: { name: apple } }",
                "    then: { result: SUCCESS }");
        failure(r);
        assertEquals(1, world.begun);
        assertEquals(1, world.ended);
    }

    @Test
    void unmetNeedsRunNothing() {
        ExampleRunner r = runner(
                "  - name: e",
                "    needs: [NotYetSpecified]",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("needs 未滿足"));
        assertEquals(0, world.begun);
        assertFalse(invoker.calls.contains("ListThings as u-1"));
    }

    @Test
    void versionTwoWithoutAWorldCannotRun() {
        Map<String, Object> doc = new Yaml().load(HEADER + String.join("\n",
                "  - name: e",
                "    when: { run: ListThings, as: owner }",
                "    then: { result: SUCCESS }") + "\n");
        ExampleRunner r = new ExampleRunner(doc, SPECIFIED, new FakeInvoker(new FakeWorld()));
        assertTrue(failure(r).contains("ExampleWorld"));
    }
}
