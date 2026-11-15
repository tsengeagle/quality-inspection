package com.tzuchi.his.quality.inspection.acceptance;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
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
 * 執行器本身的測試：該通過的通過、該失敗的失敗，而且失敗訊息指出原因。
 *
 * <p>用一個假的 use case 實作（記憶體裡的「物品」），不依賴應用程式，所以這裡的失敗只可能是執行器的錯。</p>
 */
class ExampleRunnerTest {

    private static final Set<String> SPECIFIED = new HashSet<>(Arrays.asList("CreateThing", "GetThing", "RenameThing"));

    /** 假的 use case：CreateThing 回傳 id；GetThing 回傳物品；RenameThing 名稱空白時以 VALIDATION_ERROR 拒絕。 */
    static final class FakeInvoker implements UseCaseInvoker {
        final Map<String, Map<String, Object>> things = new LinkedHashMap<>();
        int calls;

        @Override
        public Outcome invoke(String useCase, Map<String, Object> actor, Map<String, Object> input) {
            calls++;
            if (!"u-1".equals(actor.get("user"))) {
                return Outcome.rejected("ROLE_DENIED", "not the owner");
            }
            switch (useCase) {
                case "CreateThing": {
                    String id = "t-" + (things.size() + 1);
                    Map<String, Object> thing = new LinkedHashMap<>();
                    thing.put("id", id);
                    thing.put("name", input.get("name"));
                    thing.put("tags", input.get("tags"));
                    thing.put("weight", new BigDecimal("0.5"));
                    thing.put("count", 3L);
                    things.put(id, thing);
                    return Outcome.success(id);
                }
                case "GetThing": {
                    Map<String, Object> thing = things.get(String.valueOf(input.get("id")));
                    return thing == null ? Outcome.rejected("NOT_FOUND", "none") : Outcome.success(thing);
                }
                case "RenameThing": {
                    Object name = input.get("name");
                    if (name == null || String.valueOf(name).trim().isEmpty()) {
                        return Outcome.rejected("VALIDATION_ERROR", "blank");
                    }
                    things.get(String.valueOf(input.get("id"))).put("name", name);
                    return Outcome.success(null);
                }
                case "Explode":
                    return Outcome.rejected(null, "IllegalStateException: boom");
                default:
                    throw new RunnerProblem("找不到 use case " + useCase);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> properties(Object value) {
            if (!(value instanceof Map)) {
                throw new RunnerProblem("輸出是單一值（" + value + "），沒有欄位可以比對");
            }
            return (Map<String, Object>) value;
        }
    }

    private static final String HEADER = String.join("\n",
            "spec_version: 1",
            "kind: examples",
            "actors:",
            "  owner:    { user: u-1, roles: [OWNER], hospital: H-01 }",
            "  stranger: { user: u-9, roles: [OWNER], hospital: H-02 }",
            "fixtures:",
            "  thing:",
            "    steps:",
            "      - run: CreateThing",
            "        as: owner",
            "        input: { name: apple, tags: [a, b] }",
            "        save: { t: id }",
            "  needs_other:",
            "    needs: [CreateOther]",
            "    steps:",
            "      - use: thing",
            "examples:",
            "");

    private FakeInvoker invoker;

    private ExampleRunner runner(String... exampleLines) {
        invoker = new FakeInvoker();
        Map<String, Object> doc = new Yaml().load(HEADER + String.join("\n", exampleLines) + "\n");
        return new ExampleRunner(doc, SPECIFIED, invoker);
    }

    private static String failure(ExampleRunner runner) {
        AssertionError error = assertThrows(AssertionError.class, () -> runner.run("e"));
        return error.getMessage();
    }

    // --- 通過 ---

    @Test
    void passingExampleWithGivenWhenObserveAndVariables() {
        ExampleRunner r = runner(
                "  - name: e",
                "    basis: [X-A-01]",
                "    given: [{ use: thing }]",
                "    when: { run: RenameThing, as: owner, input: { id: \"${t}\", name: pear } }",
                "    then: { result: SUCCESS }",
                "    observe:",
                "      - run: GetThing",
                "        as: owner",
                "        input: { id: \"${t}\" }",
                "        expect: { result: SUCCESS, output: { name: pear, tags: [b, a], weight: \"0.50\", count: 3 } }");
        assertDoesNotThrow(() -> r.run("e"));
        assertEquals(3, invoker.calls);
    }

    @Test
    void expectedRejectionWithTheRightCodePasses() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: RenameThing, as: owner, input: { id: \"${t}\", name: \"  \" } }",
                "    then: { result: REJECTED, code: VALIDATION_ERROR }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void outputComparisonIsPartial() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS }",
                "    observe:",
                "      - { run: GetThing, as: owner, input: { id: \"${t}\" }, expect: { result: SUCCESS, output: { name: apple } } }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void variableInsideALongerStringIsReplaced() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: RenameThing, as: owner, input: { id: \"${t}\", name: \"copy of ${t}\" } }",
                "    then: { result: SUCCESS }",
                "    observe:",
                "      - { run: GetThing, as: owner, input: { id: \"${t}\" }, expect: { result: SUCCESS, output: { name: copy of t-1 } } }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    // --- 結果不符 ---

    @Test
    void successWhenRejectionExpectedFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: RenameThing, as: owner, input: { id: \"${t}\", name: pear } }",
                "    then: { result: REJECTED, code: VALIDATION_ERROR }");
        assertTrue(failure(r).contains("預期 REJECTED VALIDATION_ERROR，實際 SUCCESS"));
    }

    @Test
    void rejectionWhenSuccessExpectedFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: RenameThing, as: owner, input: { id: \"${t}\", name: \"\" } }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("預期 SUCCESS，實際 REJECTED VALIDATION_ERROR"));
    }

    @Test
    void wrongErrorCodeFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: stranger, input: { id: \"${t}\" } }",
                "    then: { result: REJECTED, code: HOSPITAL_SCOPE_MISMATCH }");
        assertTrue(failure(r).contains("預期 REJECTED HOSPITAL_SCOPE_MISMATCH，實際 REJECTED ROLE_DENIED"));
    }

    @Test
    void rejectionWithoutAnErrorCodeFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: Explode, as: owner, input: {} }",
                "    then: { result: REJECTED, code: VALIDATION_ERROR }");
        assertTrue(failure(r).contains("沒有錯誤代碼"));
    }

    @Test
    void outputValueMismatchFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS }",
                "    observe:",
                "      - { run: GetThing, as: owner, input: { id: \"${t}\" }, expect: { result: SUCCESS, output: { name: pear } } }");
        assertTrue(failure(r).contains("name 預期 pear，實際 apple"));
    }

    @Test
    void missingOutputFieldFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS }",
                "    observe:",
                "      - { run: GetThing, as: owner, input: { id: \"${t}\" }, expect: { result: SUCCESS, output: { versionNo: 1 } } }");
        assertTrue(failure(r).contains("輸出沒有欄位 versionNo"));
    }

    @Test
    void setWithADifferentMemberFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS }",
                "    observe:",
                "      - { run: GetThing, as: owner, input: { id: \"${t}\" }, expect: { result: SUCCESS, output: { tags: [a, c] } } }");
        assertTrue(failure(r).contains("tags 預期"));
    }

    @Test
    void failingObserveResultFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS }",
                "    observe:",
                "      - { run: GetThing, as: owner, input: { id: no-such }, expect: { result: SUCCESS } }");
        assertTrue(failure(r).contains("observe[0] GetThing：預期 SUCCESS，實際 REJECTED NOT_FOUND"));
    }

    @Test
    void failingGivenStepFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ run: CreateThing, as: stranger, input: { name: x } }]",
                "    when: { run: GetThing, as: owner, input: { id: t-1 } }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("準備步驟 given[0] CreateThing 失敗"));
    }

    // --- 無法執行也算未通過 ---

    @Test
    void unmetNeedsFailsWithoutRunningAnyStep() {
        ExampleRunner r = runner(
                "  - name: e",
                "    needs: [BindOther]",
                "    when: { run: GetThing, as: owner, input: { id: t-1 } }",
                "    then: { result: SUCCESS }");
        String message = failure(r);
        assertTrue(message.contains("needs 未滿足") && message.contains("BindOther"), message);
        assertEquals(0, invoker.calls);
    }

    @Test
    void needsOfAFixtureCountToo() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: needs_other }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("CreateOther"));
        assertEquals(0, invoker.calls);
    }

    @Test
    void needsThatAreSpecifiedAreSatisfied() {
        ExampleRunner r = runner(
                "  - name: e",
                "    needs: [CreateThing]",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    @Test
    void pendingInputFailsWithoutRunningAnyStep() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ run: CreateThing, as: owner, input_pending: 另一份規格 }]",
                "    when: { run: GetThing, as: owner, input: { id: t-1 } }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("輸入待補"));
        assertEquals(0, invoker.calls);
    }

    @Test
    void undefinedVariableFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: GetThing, as: owner, input: { id: \"${nope}\" } }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("變數 ${nope} 沒有定義"));
    }

    @Test
    void unknownUseCaseFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: DeleteThing, as: owner, input: {} }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("找不到 use case DeleteThing"));
    }

    @Test
    void savingAFieldTheOutputLacksFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" }, save: { v: versionId } }",
                "    then: { result: SUCCESS }");
        assertTrue(failure(r).contains("輸出沒有 versionId"));
    }

    @Test
    void unknownResultIsAProblem() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: GetThing, as: owner, input: { id: t-1 } }",
                "    then: { result: MAYBE }");
        assertTrue(failure(r).contains("result 必須是 SUCCESS 或 REJECTED"));
    }

    @Test
    void unknownExampleFails() {
        ExampleRunner r = runner(
                "  - name: e",
                "    when: { run: GetThing, as: owner, input: { id: t-1 } }",
                "    then: { result: SUCCESS }");
        assertThrows(AssertionError.class, () -> r.run("other"));
    }

    @Test
    void duplicateExampleNamesAreRejected() {
        assertThrows(RunnerProblem.class, () -> runner(
                "  - { name: e, when: { run: GetThing, as: owner, input: {} }, then: { result: SUCCESS } }",
                "  - { name: e, when: { run: GetThing, as: owner, input: {} }, then: { result: SUCCESS } }"));
    }

    @Test
    void exampleNamesMustBeIdentifiers() {
        RunnerProblem problem = assertThrows(RunnerProblem.class, () -> runner(
                "  - { name: check result, when: { run: GetThing, as: owner, input: {} }, then: { result: SUCCESS } }"));
        assertTrue(problem.getMessage().contains("範例名稱只能用"), problem.getMessage());
    }

    @Test
    void savedVariablesAreSubstitutedInExpectedOutput() {
        ExampleRunner r = runner(
                "  - name: e",
                "    given: [{ use: thing }]",
                "    when: { run: GetThing, as: owner, input: { id: \"${t}\" } }",
                "    then: { result: SUCCESS }",
                "    observe:",
                "      - { run: GetThing, as: owner, input: { id: \"${t}\" }, expect: { result: SUCCESS, output: { id: \"${t}\" } } }");
        assertDoesNotThrow(() -> r.run("e"));
    }

    // --- 值的比較 ---

    @Test
    void valueComparisonRules() {
        assertTrue(ExampleRunner.same("0.50", new BigDecimal("0.5")));
        assertTrue(ExampleRunner.same(1, 1L));
        assertFalse(ExampleRunner.same("0.50", new BigDecimal("0.51")));
        assertTrue(ExampleRunner.same(Arrays.asList("a", "b"), Arrays.asList("b", "a")));
        assertFalse(ExampleRunner.same(Arrays.asList("a", "a"), Arrays.asList("a", "b")));
        assertFalse(ExampleRunner.same(Collections.singletonList("a"), Arrays.asList("a", "b")));
        assertTrue(ExampleRunner.same(null, null));
        assertFalse(ExampleRunner.same(null, "x"));
        assertFalse(ExampleRunner.same(true, "true"));
        assertFalse(ExampleRunner.same("1", "1.0"));
        // 部分比對的物件：較寬的預期不能搶走較窄的預期唯一能配的元素，兩種順序結果要相同
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("k", 1);
        a.put("name", "a");
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("k", 1);
        b.put("name", "b");
        List<Object> want = Arrays.asList(Collections.singletonMap("k", 1), a);
        assertTrue(ExampleRunner.same(want, Arrays.asList(a, b)));
        assertTrue(ExampleRunner.same(want, Arrays.asList(b, a)));
        assertFalse(ExampleRunner.same(Arrays.asList(a, a), Arrays.asList(a, b)));
        List<Object> nested = Collections.singletonList(Collections.singletonMap("k", 1));
        assertTrue(ExampleRunner.same(nested, Collections.singletonList(Collections.singletonMap("k", 1L))));
    }
}
