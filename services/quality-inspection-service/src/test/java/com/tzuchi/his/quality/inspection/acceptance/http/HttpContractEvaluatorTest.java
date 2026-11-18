package com.tzuchi.his.quality.inspection.acceptance.http;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTTP 契約比對器（B-1：觀察）的行為。每一項都對應 docs/harness/13-http-contract-options.md 的一項設計。
 */
class HttpContractEvaluatorTest {

    private static final String COMMON = String.join("\n",
            "spec_version: 1",
            "kind: http-common",
            "envelope:",
            "  success: { success: 'true', data: $data, meta: 'null', error: 'null' }",
            "  error: { success: 'false', data: 'null', meta: 'null', error: { code: text, message: 'text?' } }",
            "status_by_category: { denied: 403, not_found: 404, invalid_input: 400, conflict: 409, rule_violation: 422 }",
            "transport_codes:",
            "  - { code: MISSING_API_KEY, status: 401 }");

    private static final String HTTP = String.join("\n",
            "spec_version: 1",
            "kind: http",
            "endpoints:",
            "  - usecase: ListThings",
            "    method: GET",
            "    path: /things",
            "    success:",
            "      status: 200",
            "      data:",
            "        items:",
            "          - thingId: id",
            "            status: enum ThingStatus",
            "            count: int",
            "            ratio: decimal",
            "            note: text?",
            "  - usecase: GetThingParts",
            "    method: GET",
            "    path: /things/{thingId}/parts",
            "    success:",
            "      status: 200",
            "      data: { thingId: id, parts: [ { partId: id } ] }",
            "  - usecase: GetThing",
            "    method: GET",
            "    path: /things/{thingId}",
            "    success: { status: 200, data: { thingId: id } }");

    private static final String USECASES = String.join("\n",
            "spec_version: 1",
            "kind: usecases",
            "error_codes:",
            "  - { code: THING_NOT_FOUND, category: not_found }",
            "  - { code: THING_LOCKED, category: rule_violation }",
            "usecases:",
            "  - name: ListThings",
            "  - name: GetThingParts",
            "  - name: GetThing");

    private static final String MODEL = String.join("\n",
            "spec_version: 1",
            "kind: aggregate",
            "enums:",
            "  - { name: ThingStatus, values: [DRAFT, PUBLISHED] }");

    private static HttpContract contract() {
        return contract(HTTP, USECASES);
    }

    private static HttpContract contract(String http, String usecases) {
        HttpContract c = HttpContract.parse(COMMON, Map.of("thing", http), Map.of("thing", usecases), Map.of("thing", MODEL));
        return c;
    }

    private static String ok(String data) {
        return "{\"success\":true,\"data\":" + data + ",\"meta\":null,\"error\":null}";
    }

    private static String err(String code) {
        return "{\"success\":false,\"data\":null,\"meta\":null,\"error\":{\"code\":\"" + code + "\",\"message\":\"m\"}}";
    }

    private static final String GOOD_LIST = ok("{\"items\":[{\"thingId\":\"T1\",\"status\":\"DRAFT\",\"count\":3,\"ratio\":0.5,\"note\":null}]}");
    private static final String GOOD_PARTS = ok("{\"thingId\":\"T1\",\"parts\":[{\"partId\":\"P1\"}]}");
    private static final String GOOD_THING = ok("{\"thingId\":\"T1\"}");

    /** 三個端點都觀察到一次正確的成功回應，陣列也都非空：沒有任何問題。 */
    private static HttpContractEvaluator fullyCovered(HttpContract c) {
        HttpContractEvaluator e = new HttpContractEvaluator(c);
        e.observe(new HttpObservation("GET", "/things", 200, GOOD_LIST));
        e.observe(new HttpObservation("GET", "/things/T1/parts", 200, GOOD_PARTS));
        e.observe(new HttpObservation("GET", "/things/T1", 200, GOOD_THING));
        return e;
    }

    private static void assertProblem(HttpContractEvaluator e, String fragment) {
        List<String> problems = e.problems();
        assertTrue(problems.stream().anyMatch(p -> p.contains(fragment)),
                "預期有含「" + fragment + "」的問題，實際：" + problems);
    }

    @Test
    void contractLoadsWithoutProblems() {
        assertEquals(List.of(), contract().loadProblems());
        assertEquals(3, contract().endpoints().size());
    }

    @Test
    void fullyCoveredCorrectResponsesHaveNoProblems() {
        assertEquals(List.of(), fullyCovered(contract()).problems());
    }

    // ---------- E-03：回應形狀 ----------

    @Test
    void e03_listReturnedAsBareArrayIsAViolation() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things", 200,
                ok("[{\"thingId\":\"T1\",\"status\":\"DRAFT\",\"count\":3,\"ratio\":0.5,\"note\":null}]")));
        assertProblem(e, "GET /things");
        assertProblem(e, "$.data：預期物件，實際是陣列");
    }

    @Test
    void e03_sectionsReturnedAsBareArrayIsAViolation() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1/parts", 200, ok("[{\"partId\":\"P1\"}]")));
        assertProblem(e, "GET /things/{thingId}/parts");
        assertProblem(e, "$.data：預期物件，實際是陣列");
    }

    @Test
    void extraFieldIsAViolation() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"hasExceptionEntry\":false}")));
        assertProblem(e, "$.data：契約沒有的欄位 hasExceptionEntry");
    }

    @Test
    void missingFieldIsAViolation() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{}")));
        assertProblem(e, "$.data：缺少欄位 thingId");
    }

    @Test
    void nullOnlyAllowedWhenNullable() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":null}")));
        assertProblem(e, "$.data.thingId：不可為 null");
    }

    @Test
    void enumValueMustBeDefinedInModel() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things", 200,
                ok("{\"items\":[{\"thingId\":\"T1\",\"status\":\"FROZEN\",\"count\":3,\"ratio\":0.5,\"note\":null}]}")));
        assertProblem(e, "$.data.items[0].status：FROZEN 不是 ThingStatus 的值");
    }

    @Test
    void intRejectsFractionDecimalAcceptsInteger() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things", 200,
                ok("{\"items\":[{\"thingId\":\"T1\",\"status\":\"DRAFT\",\"count\":1.5,\"ratio\":10,\"note\":null}]}")));
        assertProblem(e, "$.data.items[0].count：預期整數");
        assertTrue(e.problems().stream().noneMatch(p -> p.contains("ratio")), e.problems().toString());
    }

    @Test
    void envelopeIsChecked() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, "{\"thingId\":\"T1\"}"));
        assertProblem(e, "$：缺少欄位 success");
    }

    @Test
    void unexpectedSuccessStatusIsAViolation() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 201, GOOD_THING));
        assertProblem(e, "成功狀態碼應為 200，實際 201");
    }

    // ---------- 錯誤回應 ----------

    @Test
    void errorStatusMustFollowCategoryOfCode() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 404, err("THING_NOT_FOUND")));
        e.observe(new HttpObservation("GET", "/things/T1", 400, err("THING_LOCKED")));
        List<String> problems = e.problems();
        assertEquals(1, problems.size(), problems.toString());
        assertProblem(e, "THING_LOCKED 的分類是 rule_violation，狀態碼應為 422，實際 400");
    }

    @Test
    void transportCodeUsesItsOwnStatus() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 401, err("MISSING_API_KEY")));
        assertEquals(List.of(), e.problems());
    }

    @Test
    void undefinedErrorCodeIsAViolation() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 500, err("INTERNAL_ERROR")));
        assertProblem(e, "錯誤代碼 INTERNAL_ERROR 沒有定義");
    }

    @Test
    void errorEnvelopeIsChecked() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 404, "{\"success\":false}"));
        assertProblem(e, "$：缺少欄位 error");
    }

    // ---------- B-1 的配套：涵蓋 ----------

    @Test
    void endpointWithoutObservedSuccessIsAProblem() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract());
        e.observe(new HttpObservation("GET", "/things", 200, GOOD_LIST));
        e.observe(new HttpObservation("GET", "/things/T1/parts", 200, GOOD_PARTS));
        e.observe(new HttpObservation("GET", "/things/T1", 404, err("THING_NOT_FOUND")));
        assertProblem(e, "GET /things/{thingId}：沒有觀察到任何成功回應");
    }

    @Test
    void arrayNeverObservedNonEmptyIsAProblem() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract());
        e.observe(new HttpObservation("GET", "/things", 200, ok("{\"items\":[]}")));
        e.observe(new HttpObservation("GET", "/things/T1/parts", 200, GOOD_PARTS));
        e.observe(new HttpObservation("GET", "/things/T1", 200, GOOD_THING));
        assertProblem(e, "GET /things：陣列 $.data.items 從未觀察到非空");
    }

    // ---------- 路徑對應 ----------

    @Test
    void pathMatchingUsesSegmentsAndIgnoresQueryString() {
        HttpContract c = contract();
        assertEquals("/things/{thingId}/parts", c.match("GET", "/things/T1/parts").get().path());
        assertEquals("/things/{thingId}", c.match("GET", "/things/T1").get().path());
        assertEquals("/things", c.match("GET", "/things?hospitalId=H1").get().path());
        assertTrue(c.match("POST", "/things").isEmpty());
        assertTrue(c.match("GET", "/others").isEmpty());
    }

    @Test
    void responsesToEndpointsOutsideTheContractAreIgnored() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/others", 200, "[]"));
        assertEquals(List.of(), e.problems());
    }

    // ---------- 契約本身 ----------

    @Test
    void endpointMustBelongToAUseCaseOfItsAggregate() {
        HttpContract c = contract(HTTP.replace("usecase: GetThing\n", "usecase: GetOtherThing\n"), USECASES);
        assertTrue(c.loadProblems().stream().anyMatch(p -> p.contains("GetOtherThing 不在 thing 的 usecases.yaml")),
                c.loadProblems().toString());
    }

    @Test
    void unknownShapeTypeIsALoadProblem() {
        HttpContract c = contract(HTTP.replace("count: int", "count: integer"), USECASES);
        assertTrue(c.loadProblems().stream().anyMatch(p -> p.contains("不認得的型別 integer")),
                c.loadProblems().toString());
    }

    @Test
    void unknownEnumIsALoadProblem() {
        HttpContract c = contract(HTTP.replace("enum ThingStatus", "enum ThingState"), USECASES);
        assertTrue(c.loadProblems().stream().anyMatch(p -> p.contains("ThingState 沒有定義")),
                c.loadProblems().toString());
    }

    @Test
    void duplicateEndpointIsALoadProblem() {
        String dup = HTTP + "\n  - usecase: GetThing\n    method: GET\n    path: /things/{id}\n    success: { status: 200, data: { thingId: id } }";
        HttpContract c = contract(dup, USECASES);
        assertTrue(c.loadProblems().stream().anyMatch(p -> p.contains("重複的端點 GET /things/{}")),
                c.loadProblems().toString());
    }

    // ---------- Copilot review（#93）----------

    @Test
    void trailingJsonAfterTheBodyIsAViolation() {
        HttpContractEvaluator e = fullyCovered(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, GOOD_THING + "[]"));
        assertProblem(e, "GET /things/{thingId}");
        assertProblem(e, "回應不是單一 JSON 文件");
    }

    @Test
    void invalidShapesAreLoadProblemsAndObservingNeverThrows() {
        String broken = HTTP.replace("      data: { thingId: id, parts: [ { partId: id } ] }", "      data: { thingId: id, parts: [] }")
                .replace("count: int", "count: true");
        HttpContract c = contract(broken, USECASES);
        assertTrue(c.loadProblems().stream().anyMatch(p -> p.contains("陣列的形狀必須剛好一個元素")), c.loadProblems().toString());
        assertTrue(c.loadProblems().stream().anyMatch(p -> p.contains("形狀必須是對應表、清單或字串")), c.loadProblems().toString());
        HttpContractEvaluator e = fullyCovered(c);
        e.observe(new HttpObservation("GET", "/things/T1/parts", 200, GOOD_PARTS));
        assertTrue(e.problems().containsAll(c.loadProblems()), e.problems().toString());
    }
}
