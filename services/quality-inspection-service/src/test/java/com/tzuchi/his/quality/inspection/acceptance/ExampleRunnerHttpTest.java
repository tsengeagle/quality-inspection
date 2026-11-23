package com.tzuchi.his.quality.inspection.acceptance;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 執行器對 http 步驟（D-074，規格格式 v2.1）的測試：該通過的通過、該失敗的失敗，而且失敗訊息指出原因。
 *
 * <p>用假的 use case、假的環境與假的 HTTP，不依賴應用程式，所以這裡的失敗只可能是執行器的錯。</p>
 */
class ExampleRunnerHttpTest {

    private static final Set<String> SPECIFIED = new HashSet<>(Arrays.asList("CreateThing", "GetThing"));

    /** 假的 HTTP：記下收到的請求，回應由測試指定。 */
    static final class FakeHttp implements HttpInvoker {
        final List<Request> requests = new ArrayList<>();
        Function<Request, Reply> respond = request -> envelope(200, null);
        String notReady;

        @Override public void checkReady() {
            if (notReady != null) {
                throw new RunnerProblem(notReady);
            }
        }

        @Override public Reply send(Request request) {
            requests.add(request);
            return respond.apply(request);
        }
    }

    private static HttpInvoker.Reply envelope(int status, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        body.put("error", null);
        return new HttpInvoker.Reply(status, "{...}", body);
    }

    private static HttpInvoker.Reply error(int status, String code) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", "no");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("data", null);
        body.put("error", error);
        return new HttpInvoker.Reply(status, "{\"error\":\"" + code + "\"}", body);
    }

    private static final String HEADER = String.join("\n",
            "spec_version: 2",
            "kind: examples",
            "actors:",
            "  owner:   { user: u-1, roles: [OWNER], hospital: H-01 }",
            "  both:    { user: u-2, roles: [OWNER, READER] }",
            "  visitor: { user: u-3, roles: [VISITOR], hospital: H-01 }",
            "fixtures:",
            "  thing:",
            "    steps:",
            "      - run: CreateThing",
            "        as: owner",
            "        input: { name: apple }",
            "        save: { t: id }",
            "examples:",
            "");

    private static final String PROFILE = String.join("\n",
            "kind: spec-profile",
            "profile_version: 2",
            "clock_start: \"2026-11-19T17:00:00+08:00\"",
            "local_time_zone: Asia/Taipei",
            "role_claims: { OWNER: owner, READER: reader }",
            "");

    private static final String HTTP_COMMON = String.join("\n",
            "status_by_category: { denied: 403, invalid_input: 400, rule_violation: 422 }",
            "transport_codes:",
            "  - { code: UNAUTHORIZED, status: 401 }",
            "  - { code: VALIDATION_ERROR, status: 400 }",
            "  - { code: TWO_ANSWERS, status: 409 }",
            "");

    private static final Map<String, String> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("ROLE_DENIED", "denied");
        CATEGORIES.put("THING_LOCKED", "rule_violation");
        CATEGORIES.put("TWO_ANSWERS", "rule_violation");
        CATEGORIES.put("ODD", "unknown_category");
    }

    private ExampleRunnerV2Test.FakeWorld world;
    private ExampleRunnerV2Test.FakeInvoker invoker;
    private FakeHttp http;

    private ExampleRunner runner(String... exampleLines) {
        http = new FakeHttp();
        return runnerWith(http, exampleLines);
    }

    private ExampleRunner runnerWith(HttpInvoker httpInvoker, String... exampleLines) {
        world = new ExampleRunnerV2Test.FakeWorld();
        invoker = new ExampleRunnerV2Test.FakeInvoker(world);
        Map<String, Map<String, Object>> documents = new LinkedHashMap<>();
        documents.put("home", new Yaml().load(HEADER + String.join("\n", exampleLines) + "\n"));
        Map<String, Object> profile = new Yaml().load(PROFILE);
        Map<String, Object> common = new Yaml().load(HTTP_COMMON);
        return new ExampleRunner("home", documents, profile, SPECIFIED, invoker, world, httpInvoker, common, CATEGORIES);
    }

    private static String failure(ExampleRunner runner) {
        AssertionError error = assertThrows(AssertionError.class, () -> runner.run("e"));
        return error.getMessage();
    }

    // --- 請求 ---

    @Test
    void sendsTheRequestBuiltFromTheStep() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when:",
                "      http:",
                "        method: POST",
                "        path: \"/things/${t}/rename\"",
                "        body: { id: \"${t}\", name: pear }",
                "        query: { dryRun: true, ref: \"${t}\" }",
                "      as: both",
                "    then: { result: SUCCESS }");
        assertDoesNotThrow(() -> r.run("e"));
        assertEquals(1, http.requests.size());
        HttpInvoker.Request sent = http.requests.get(0);
        assertEquals("POST", sent.method);
        assertEquals("/things/t-1/rename", sent.path);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", "t-1");
        body.put("name", "pear");
        assertEquals(body, sent.body);
        assertNull(sent.bodyRaw);
        assertEquals(Boolean.TRUE, sent.query.get("dryRun"));
        assertEquals("t-1", sent.query.get("ref"));
        assertFalse(sent.anonymous);
        assertEquals("u-2", sent.user);
        assertNull(sent.hospital);
        assertEquals(Arrays.asList("owner", "reader"), sent.roleClaims);
    }

    @Test
    void bodyRawIsSentUntouched() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when:",
                "      http: { method: POST, path: \"/things/${t}\", body_raw: '{\"id\":\"${t}\",\"at\":\"2026-11-26T23:59:00\"' }",
                "      as: owner",
                "    then: { result: REJECTED, code: VALIDATION_ERROR }");
        http.respond = request -> error(400, "VALIDATION_ERROR");
        assertDoesNotThrow(() -> r.run("e"));
        HttpInvoker.Request sent = http.requests.get(0);
        assertEquals("{\"id\":\"${t}\",\"at\":\"2026-11-26T23:59:00\"", sent.bodyRaw);
        assertNull(sent.body);
        assertEquals("H-01", sent.hospital);
    }

    @Test
    void anonymousSendsNoActor() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: GET, path: /things }, as: anonymous }",
                "    then: { result: REJECTED, code: UNAUTHORIZED }");
        http.respond = request -> error(401, "UNAUTHORIZED");
        assertDoesNotThrow(() -> r.run("e"));
        assertTrue(http.requests.get(0).anonymous);
        assertNull(http.requests.get(0).user);
        assertTrue(http.requests.get(0).roleClaims.isEmpty());
    }

    @Test
    void aRoleWithoutAClaimCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: GET, path: /things }, as: visitor }",
                "    then: { result: SUCCESS }");
        String message = failure(r);
        assertTrue(message.contains("無法執行"), message);
        assertTrue(message.contains("role_claims 沒有角色 VISITOR"), message);
        assertTrue(http.requests.isEmpty());
    }

    @Test
    void anUnknownActorCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: GET, path: /things }, as: nobody }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("actors 裡沒有操作者 nobody"));
    }

    @Test
    void bodyAndBodyRawTogetherCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: POST, path: /things, body: { a: 1 }, body_raw: '{' }, as: owner }",
                "    then: { result: REJECTED, code: VALIDATION_ERROR }");
        assertTrue(failure(r).contains("只能擇一"));
    }

    // --- 環境 ---

    @Test
    void stopsBeforeAnyStepWhenTheEnvironmentIsNotReady() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { http: { method: GET, path: \"/things/${t}\" }, as: owner }",
                "    then: { result: SUCCESS }");
        http.notReady = "院區被設定覆寫";
        String message = failure(r);
        assertTrue(message.contains("無法執行"), message);
        assertTrue(message.contains("院區被設定覆寫"), message);
        assertTrue(invoker.calls.isEmpty(), "準備步驟不應該執行");
        assertTrue(http.requests.isEmpty());
        assertEquals(0, world.begun);
    }

    @Test
    void runExamplesDoNotCheckTheHttpEnvironment() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: owner, input: { name: apple } }",
                "    then: { result: SUCCESS }");
        http.notReady = "院區被設定覆寫";
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void anHttpStepWithoutAnHttpInvokerCannotRun() {
        ExampleRunner r = runnerWith(null,
                "  - name: e",
                "    when: { http: { method: GET, path: /things }, as: owner }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("http 步驟需要送出請求的環境"));
    }

    @Test
    void httpIsOnlyAllowedInWhen() {
        ExampleRunner given = runner(
                "  - name: e",
                "    given: [{ http: { method: POST, path: /things }, as: owner }]",
                "    when: { run: CreateThing, as: owner, input: { name: apple } }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(given).contains("http 步驟只能用在 when"));
        assertTrue(http.requests.isEmpty());

        ExampleRunner observe = runner(
                "  - name: e",
                "    when: { run: CreateThing, as: owner, input: { name: apple } }",
                "    then: { result: SUCCESS }",
                "    observe: [{ http: { method: GET, path: /things }, as: owner }]");
        assertTrue(failure(observe).contains("http 步驟只能用在 when"));
        assertTrue(http.requests.isEmpty());
    }

    // --- 被拒絕：代碼與狀態碼 ---

    @Test
    void rejectedWithATransportCodeChecksItsStatus() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: GET, path: /things }, as: anonymous }",
                "    then: { result: REJECTED, code: UNAUTHORIZED }");
        http.respond = request -> error(403, "UNAUTHORIZED");
        String message = failure(r);
        assertTrue(message.contains("UNAUTHORIZED 的狀態碼應為 401"), message);
        assertTrue(message.contains("實際 403"), message);
    }

    @Test
    void rejectedWithACategoryCodeChecksTheStatusOfTheCategory() {
        String[] example = {
                "  - name: e",
                "    when: { http: { method: POST, path: /things/t-1/lock }, as: owner }",
                "    then: { result: REJECTED, code: THING_LOCKED }"};
        ExampleRunner passes = runner(example);
        http.respond = request -> error(422, "THING_LOCKED");
        assertDoesNotThrow(() -> passes.run("e"));

        ExampleRunner fails = runner(example);
        http.respond = request -> error(409, "THING_LOCKED");
        String message = failure(fails);
        assertTrue(message.contains("THING_LOCKED 的狀態碼應為 422（分類 rule_violation）"), message);
    }

    @Test
    void aDifferentCodeFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: POST, path: /things }, as: owner }",
                "    then: { result: REJECTED, code: ROLE_DENIED }");
        http.respond = request -> error(403, "THING_LOCKED");
        String message = failure(r);
        assertTrue(message.contains("預期 REJECTED ROLE_DENIED"), message);
        assertTrue(message.contains("THING_LOCKED"), message);
    }

    @Test
    void aRejectionWithoutAnEnvelopeFailsAnExampleThatExpectsACode() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: POST, path: /things, body_raw: '{' }, as: owner }",
                "    then: { result: REJECTED, code: VALIDATION_ERROR }");
        http.respond = request -> new HttpInvoker.Reply(400, "", null);
        assertTrue(failure(r).contains("實際被拒絕但沒有錯誤代碼"));
    }

    @Test
    void expectedRejectionButSucceededFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: POST, path: /things }, as: owner }",
                "    then: { result: REJECTED, code: ROLE_DENIED }");
        assertTrue(failure(r).contains("預期 REJECTED ROLE_DENIED，實際 SUCCESS"));
    }

    @Test
    void aCodeWithNoStatusToInferCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: POST, path: /things }, as: owner }",
                "    then: { result: REJECTED, code: NOWHERE }");
        http.respond = request -> error(400, "NOWHERE");
        String message = failure(r);
        assertTrue(message.contains("無法執行"), message);
        assertTrue(message.contains("推不出錯誤代碼 NOWHERE 的狀態碼"), message);
    }

    @Test
    void aCodeWhoseCategoryHasNoStatusCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: POST, path: /things }, as: owner }",
                "    then: { result: REJECTED, code: ODD }");
        http.respond = request -> error(400, "ODD");
        assertTrue(failure(r).contains("status_by_category 沒有分類 unknown_category"));
    }

    @Test
    void aCodeWithTwoDifferentStatusesCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: POST, path: /things }, as: owner }",
                "    then: { result: REJECTED, code: TWO_ANSWERS }");
        http.respond = request -> error(409, "TWO_ANSWERS");
        assertTrue(failure(r).contains("狀態碼有兩個說法"));
    }

    // --- 成功 ---

    @Test
    void successComparesTheDataOfTheEnvelope() {
        String[] example = {
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { http: { method: GET, path: \"/things/${t}\" }, as: owner }",
                "    then: { result: SUCCESS, output: { id: \"${t}\", name: NAME }, absent: [secret] }"};
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "t-1");
        data.put("name", "apple");

        ExampleRunner passes = runner(String.join("\n", example).replace("NAME", "apple"));
        http.respond = request -> envelope(200, data);
        assertDoesNotThrow(() -> passes.run("e"));

        ExampleRunner fails = runner(String.join("\n", example).replace("NAME", "pear"));
        http.respond = request -> envelope(200, data);
        assertTrue(failure(fails).contains("name 預期 pear，實際 apple"));
    }

    @Test
    void expectedSuccessButRejectedFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: GET, path: /things }, as: owner }",
                "    then: { result: SUCCESS }");
        http.respond = request -> error(403, "ROLE_DENIED");
        String message = failure(r);
        assertTrue(message.contains("預期 SUCCESS，實際 REJECTED ROLE_DENIED"), message);
        assertTrue(message.contains("HTTP 403"), message);
    }

    @Test
    void aSuccessWithoutAnEnvelopeFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: GET, path: /things }, as: owner }",
                "    then: { result: SUCCESS }");
        http.respond = request -> new HttpInvoker.Reply(204, "", null);
        assertTrue(failure(r).contains("本文不是回應外殼"));
    }

    // --- 事件 ---

    @Test
    void emitsAreCheckedLikeARunStep() {
        String[] example = {
                "  - name: e",
                "    when: { http: { method: POST, path: /things, body: { name: apple } }, as: owner }",
                "    then: { result: SUCCESS, emits: [{ event: ThingCreated, with: { id: t-1 } }] }"};
        ExampleRunner passes = runner(example);
        http.respond = request -> {
            world.emit("ThingCreated", "id", "t-1");
            return envelope(201, null);
        };
        assertDoesNotThrow(() -> passes.run("e"));

        ExampleRunner fails = runner(example);
        assertTrue(failure(fails).contains("then.emits：預期剛好發出"));
    }

    @Test
    void aRejectionThatEmitsAnEventFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: POST, path: /things, body: { name: apple } }, as: owner }",
                "    then: { result: REJECTED, code: THING_LOCKED }");
        http.respond = request -> {
            world.emit("ThingCreated", "id", "t-1");
            return error(422, "THING_LOCKED");
        };
        assertTrue(failure(r).contains("被拒絕卻發出了事件"));
    }

    // --- 沒有外殼的回應 ---

    private static final String[] BARE = {
            "  - name: e",
            "    given: [{ use: thing }]",
            "    when: { http: { method: POST, path: \"/things/${t}\", body_raw: '{\"name\":' }, as: owner }",
            "    then: { status: 400, body: empty }",
            "    observe:",
            "      - { run: GetThing, as: owner, input: { id: \"${t}\" }, expect: { result: SUCCESS, output: { name: OBSERVED } } }"};

    private static String[] bare(String observedName) {
        return String.join("\n", BARE).replace("OBSERVED", observedName).split("\n");
    }

    @Test
    void aBareReplyMatchesTheStatusAndAnEmptyBody() {
        ExampleRunner r = runner(bare("apple"));
        http.respond = request -> new HttpInvoker.Reply(400, "", null);
        assertDoesNotThrow(() -> r.run("e"));
        assertEquals(1, world.ended);
    }

    @Test
    void aBareReplyWithAnotherStatusFails() {
        ExampleRunner r = runner(bare("apple"));
        http.respond = request -> new HttpInvoker.Reply(500, "", null);
        assertTrue(failure(r).contains("預期狀態碼 400，實際 500"));
    }

    @Test
    void aBareReplyWithABodyFails() {
        ExampleRunner r = runner(bare("apple"));
        http.respond = request -> error(400, "VALIDATION_ERROR");
        assertTrue(failure(r).contains("預期回應沒有本文"));
    }

    @Test
    void aBareRejectionThatEmitsAnEventFails() {
        ExampleRunner r = runner(bare("apple"));
        http.respond = request -> {
            world.emit("ThingRenamed", "id", "t-1");
            return new HttpInvoker.Reply(400, "", null);
        };
        assertTrue(failure(r).contains("被拒絕卻發出了事件"));
    }

    @Test
    void observeStillRunsAfterABareReply() {
        ExampleRunner r = runner(bare("pear"));
        http.respond = request -> new HttpInvoker.Reply(400, "", null);
        assertTrue(failure(r).contains("name 預期 pear，實際 apple"));
    }

    @Test
    void aBareThenWithOtherFieldsCannotRun() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { http: { method: POST, path: /things, body_raw: '{' }, as: owner }",
                "    then: { status: 400, body: empty, result: REJECTED }");
        http.respond = request -> new HttpInvoker.Reply(400, "", null);
        assertTrue(failure(r).contains("then 只能寫 status（整數）與 body: empty"));
        assertEquals(Collections.emptyList(), world.events);
    }
}
