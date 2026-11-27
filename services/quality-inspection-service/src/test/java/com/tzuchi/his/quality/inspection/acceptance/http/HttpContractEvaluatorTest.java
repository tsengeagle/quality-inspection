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
    private static final String GOOD_LIST_WITH_NOTE = ok("{\"items\":[{\"thingId\":\"T2\",\"status\":\"PUBLISHED\",\"count\":1,\"ratio\":1,\"note\":\"n\"}]}");
    private static final String GOOD_PARTS = ok("{\"thingId\":\"T1\",\"parts\":[{\"partId\":\"P1\"}]}");
    private static final String GOOD_THING = ok("{\"thingId\":\"T1\"}");

    /** 三個端點都觀察到一次正確的成功回應，陣列也都非空：沒有任何問題。 */
    private static HttpContractEvaluator fullyCovered(HttpContract c) {
        HttpContractEvaluator e = new HttpContractEvaluator(c);
        e.observe(new HttpObservation("GET", "/things", 200, GOOD_LIST));
        e.observe(new HttpObservation("GET", "/things", 200, GOOD_LIST_WITH_NOTE));
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

    // ---------- W-7（D-061 草案 Q-4）：可為 null 的欄位兩種值都要觀察到 ----------

    @Test
    void nullableFieldNeverObservedNonNullIsAProblem() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract());
        e.observe(new HttpObservation("GET", "/things", 200, GOOD_LIST));
        e.observe(new HttpObservation("GET", "/things/T1/parts", 200, GOOD_PARTS));
        e.observe(new HttpObservation("GET", "/things/T1", 200, GOOD_THING));
        assertProblem(e, "GET /things：可為 null 的欄位 $.data.items[].note 從未觀察到非 null 的值");
    }

    @Test
    void nullableFieldNeverObservedNullIsAProblem() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract());
        e.observe(new HttpObservation("GET", "/things", 200, GOOD_LIST_WITH_NOTE));
        e.observe(new HttpObservation("GET", "/things/T1/parts", 200, GOOD_PARTS));
        e.observe(new HttpObservation("GET", "/things/T1", 200, GOOD_THING));
        assertProblem(e, "GET /things：可為 null 的欄位 $.data.items[].note 從未觀察到 null");
    }

    @Test
    void coverageProblemsAreSeparateFromObservationProblems() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{}")));
        assertTrue(e.observationProblems().stream().anyMatch(p -> p.contains("缺少欄位 thingId")), e.observationProblems().toString());
        assertTrue(e.observationProblems().stream().noneMatch(p -> p.contains("沒有觀察到任何成功回應")), e.observationProblems().toString());
        assertTrue(e.coverageProblems().stream().anyMatch(p -> p.contains("GET /things：沒有觀察到任何成功回應")), e.coverageProblems().toString());
    }

    // ---------- D-088：對應表的鍵結尾加 ?，欄位的值（物件、陣列或純量）可以是 null ----------

    private static final String NULLABLE_HTTP = String.join("\n",
            "spec_version: 1",
            "kind: http",
            "endpoints:",
            "  - usecase: GetThing",
            "    method: GET",
            "    path: /things/{thingId}",
            "    success:",
            "      status: 200",
            "      data:",
            "        thingId: id",
            "        owner?: { userId: id, name: text }",
            "        tags?: [ text ]");

    private static HttpContract nullableContract() {
        return contract(NULLABLE_HTTP, USECASES);
    }

    @Test
    void nullableKeyAcceptsNullAndCoversBothValues() {
        HttpContract c = nullableContract();
        assertTrue(c.loadProblems().isEmpty(), c.loadProblems().toString());
        HttpContractEvaluator e = new HttpContractEvaluator(c);
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"owner\":null,\"tags\":null}")));
        e.observe(new HttpObservation("GET", "/things/T2", 200,
                ok("{\"thingId\":\"T2\",\"owner\":{\"userId\":\"u1\",\"name\":\"n\"},\"tags\":[\"a\"]}")));
        List<String> problems = e.problems().stream().filter(p -> p.startsWith("GET /things/{thingId}")).collect(java.util.stream.Collectors.toList());
        assertTrue(problems.isEmpty(), problems.toString());
    }

    @Test
    void nullableKeyStillChecksTheValueShape() {
        HttpContractEvaluator e = new HttpContractEvaluator(nullableContract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"owner\":{\"userId\":\"u1\"},\"tags\":null}")));
        assertProblem(e, "$.data.owner：缺少欄位 name");
    }

    @Test
    void nullableKeyIsStillRequiredAndNamedWithoutTheMark() {
        HttpContractEvaluator e = new HttpContractEvaluator(nullableContract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"tags\":null,\"owner?\":null}")));
        assertProblem(e, "$.data：缺少欄位 owner");
        assertProblem(e, "$.data：契約沒有的欄位 owner?");
    }

    @Test
    void nullableKeyNeverObservedNullOrNonNullIsAProblem() {
        HttpContractEvaluator e = new HttpContractEvaluator(nullableContract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"owner\":null,\"tags\":[\"a\"]}")));
        assertProblem(e, "可為 null 的欄位 $.data.owner 從未觀察到非 null 的值");
        assertProblem(e, "可為 null 的欄位 $.data.tags 從未觀察到 null");
    }

    @Test
    void nullableKeyWithNullableScalarIsALoadProblem() {
        HttpContract c = contract(NULLABLE_HTTP.replace("tags?: [ text ]", "note?: 'text?'"), USECASES);
        assertTrue(c.loadProblems().stream().anyMatch(p -> p.contains("note：鍵結尾的 ? 不能與 text? 一起用")), c.loadProblems().toString());
    }

    // ---------- D-088：一個端點有幾種形狀（variants），每筆回應剛好符合其中一種 ----------

    private static final String VARIANT_HTTP = String.join("\n",
            "spec_version: 1",
            "kind: http",
            "endpoints:",
            "  - usecase: GetThing",
            "    method: GET",
            "    path: /things/{thingId}",
            "    success:",
            "      status: 200",
            "      variants:",
            "        - name: full",
            "          data: { thingId: id, secret: text, note: 'text?' }",
            "        - name: masked",
            "          data: { thingId: id, maskedFields: [ text ] }");

    private static HttpContract variantContract() {
        return contract(VARIANT_HTTP, USECASES);
    }

    private static List<String> problemsOf(HttpContractEvaluator e, String key) {
        return e.problems().stream().filter(p -> p.startsWith(key)).collect(java.util.stream.Collectors.toList());
    }

    @Test
    void variantsMatchEachResponseToExactlyOneShape() {
        HttpContract c = variantContract();
        assertTrue(c.loadProblems().isEmpty(), c.loadProblems().toString());
        HttpContractEvaluator e = new HttpContractEvaluator(c);
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"secret\":\"s\",\"note\":null}")));
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"secret\":\"s\",\"note\":\"n\"}")));
        e.observe(new HttpObservation("GET", "/things/T2", 200, ok("{\"thingId\":\"T2\",\"maskedFields\":[\"secret\"]}")));
        assertTrue(problemsOf(e, "GET /things/{thingId}").isEmpty(), e.problems().toString());
    }

    @Test
    void aResponseMatchingNoVariantReportsTheClosestOne() {
        HttpContractEvaluator e = new HttpContractEvaluator(variantContract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"maskedFields\":[\"secret\"],\"secret\":\"s\"}")));
        assertProblem(e, "不符合任何一種形狀；最接近的是 masked");
        assertProblem(e, "$.data：契約沒有的欄位 secret");
    }

    @Test
    void eachVariantMustBeObservedAndCoveredOnItsOwn() {
        HttpContractEvaluator e = new HttpContractEvaluator(variantContract());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"secret\":\"s\",\"note\":null}")));
        assertProblem(e, "GET /things/{thingId}（masked）：沒有觀察到這種形狀的成功回應");
        assertProblem(e, "GET /things/{thingId}（full）：可為 null 的欄位 $.data.note 從未觀察到非 null 的值");
    }

    // ---------- D-094：過渡中的形狀（transition）不檢查涵蓋，但回應仍要剛好符合一種 ----------

    private static final String TRANSITION_HTTP = VARIANT_HTTP.replace(
            "        - name: masked\n",
            "        - name: masked\n          transition: D-094\n");

    @Test
    void aTransitionVariantIsNotRequiredToBeObservedButIsListed() {
        HttpContract c = contract(TRANSITION_HTTP, USECASES);
        assertTrue(c.loadProblems().isEmpty(), c.loadProblems().toString());
        HttpContractEvaluator e = new HttpContractEvaluator(c);
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"secret\":\"s\",\"note\":null}")));
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"secret\":\"s\",\"note\":\"n\"}")));
        assertTrue(e.coverageProblems().isEmpty(), e.coverageProblems().toString());
        assertEquals(List.of("GET /things/{thingId}（masked）：D-094 過渡中，涵蓋沒有評估（觀察到 0 次）"), e.transitions());
    }

    @Test
    void aTransitionVariantStillTakesPartInMatching() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract(TRANSITION_HTTP, USECASES));
        e.observe(new HttpObservation("GET", "/things/T2", 200, ok("{\"thingId\":\"T2\",\"maskedFields\":[\"secret\"]}")));
        e.observe(new HttpObservation("GET", "/things/T3", 200, ok("{\"thingId\":\"T3\",\"other\":1}")));
        assertProblem(e, "不符合任何一種形狀");
        assertTrue(e.transitions().get(0).endsWith("（觀察到 1 次）"), e.transitions().toString());
    }

    @Test
    void theNonTransitionVariantsAreStillCovered() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract(TRANSITION_HTTP, USECASES));
        e.observe(new HttpObservation("GET", "/things/T2", 200, ok("{\"thingId\":\"T2\",\"maskedFields\":[\"secret\"]}")));
        assertProblem(e, "GET /things/{thingId}（full）：沒有觀察到這種形狀的成功回應");
    }

    @Test
    void aTransitionMustNameADecision() {
        HttpContract c = contract(TRANSITION_HTTP.replace("transition: D-094", "transition: soon"), USECASES);
        assertTrue(c.loadProblems().stream().anyMatch(p -> p.contains("transition 必須是裁決編號")), c.loadProblems().toString());
        HttpContract extra = contract(TRANSITION_HTTP.replace("transition: D-094", "transition: D-094\n          note: x"), USECASES);
        assertTrue(extra.loadProblems().stream().anyMatch(p -> p.contains("可以另有 transition")), extra.loadProblems().toString());
    }

    // ---------- D-100：幾種形狀只差在陣列的項目時，空的陣列兩種都符合，不算違反 ----------

    private static String listVariants(String full, String meta) {
        return String.join("\n",
                "spec_version: 1",
                "kind: http",
                "endpoints:",
                "  - usecase: GetThing",
                "    method: GET",
                "    path: /things/{thingId}",
                "    success:",
                "      status: 200",
                "      variants:",
                "        - name: full",
                "          data: " + full,
                "        - name: meta",
                "          data: " + meta);
    }

    private static final String LIST_VARIANT_HTTP = listVariants(
            "{ items: [ { id: id, secret: text } ], total: int }",
            "{ items: [ { id: id } ], total: int }");

    @Test
    void anEmptyArrayMatchingSeveralVariantsIsNotAViolationAndCountsForNone() {
        HttpContract c = contract(LIST_VARIANT_HTTP, USECASES);
        assertTrue(c.loadProblems().isEmpty(), c.loadProblems().toString());
        HttpContractEvaluator e = new HttpContractEvaluator(c);
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"items\":[],\"total\":0}")));
        assertTrue(e.problems().stream().noneMatch(p -> p.contains("同時符合")), e.problems().toString());
        assertProblem(e, "GET /things/{thingId}（full）：沒有觀察到這種形狀的成功回應");
        assertProblem(e, "GET /things/{thingId}（meta）：沒有觀察到這種形狀的成功回應");
    }

    @Test
    void aNonEmptyArrayIsStillMatchedToExactlyOneVariant() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract(LIST_VARIANT_HTTP, USECASES));
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"items\":[],\"total\":0}")));
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"items\":[{\"id\":\"a\"}],\"total\":1}")));
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"items\":[{\"id\":\"a\",\"secret\":\"s\"}],\"total\":1}")));
        assertTrue(problemsOf(e, "GET /things/{thingId}").isEmpty(), e.problems().toString());
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"items\":[{\"id\":\"a\",\"other\":1}],\"total\":1}")));
        assertProblem(e, "不符合任何一種形狀");
    }

    @Test
    void anEmptyArrayDoesNotExcuseVariantsThatAlsoDifferElsewhere() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract(listVariants(
                "{ items: [ { id: id, secret: text } ], note: text }",
                "{ items: [ { id: id } ], note: 'text?' }"), USECASES));
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"items\":[],\"note\":\"n\"}")));
        assertProblem(e, "同時符合不只一種形狀（full、meta）");
    }

    @Test
    void anEmptyArrayInsideAnArrayItemIsNotExcused() {
        HttpContractEvaluator e = new HttpContractEvaluator(contract(listVariants(
                "{ groups: [ { items: [ { id: id, secret: text } ] } ] }",
                "{ groups: [ { items: [ { id: id } ] } ] }"), USECASES));
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"groups\":[{\"items\":[]}]}")));
        assertProblem(e, "同時符合不只一種形狀（full、meta）");
    }

    @Test
    void ambiguousVariantsAreAProblem() {
        HttpContract c = contract(VARIANT_HTTP.replace("{ thingId: id, maskedFields: [ text ] }", "{ thingId: id, secret: text, note: 'text?' }"), USECASES);
        HttpContractEvaluator e = new HttpContractEvaluator(c);
        e.observe(new HttpObservation("GET", "/things/T1", 200, ok("{\"thingId\":\"T1\",\"secret\":\"s\",\"note\":null}")));
        assertProblem(e, "同時符合不只一種形狀（full、masked）");
    }

    @Test
    void variantsNeedAtLeastTwoNamedShapesAndExcludeData() {
        HttpContract one = contract(VARIANT_HTTP.replace(String.join("\n",
                "        - name: masked",
                "          data: { thingId: id, maskedFields: [ text ] }"), ""), USECASES);
        assertTrue(one.loadProblems().stream().anyMatch(p -> p.contains("variants 至少要有兩種形狀")), one.loadProblems().toString());
        HttpContract both = contract(VARIANT_HTTP.replace("      variants:", "      data: { thingId: id }\n      variants:"), USECASES);
        assertTrue(both.loadProblems().stream().anyMatch(p -> p.contains("data 或 variants 其中一個")), both.loadProblems().toString());
    }

    @Test
    void useCaseSpecsInFormatV2AreReadable() {
        HttpContract c = HttpContract.parse(COMMON, Map.of("thing", HTTP),
                Map.of("thing", USECASES.replace("spec_version: 1", "spec_version: 2")),
                Map.of("thing", MODEL.replace("spec_version: 1", "spec_version: 2")));
        assertTrue(c.loadProblems().isEmpty(), c.loadProblems().toString());
        HttpContract bad = HttpContract.parse(COMMON, Map.of("thing", HTTP.replace("spec_version: 1", "spec_version: 2")),
                Map.of("thing", USECASES), Map.of("thing", MODEL));
        assertTrue(bad.loadProblems().stream().anyMatch(p -> p.contains("thing/http.yaml：spec_version 必須是 1")), bad.loadProblems().toString());
    }
}
