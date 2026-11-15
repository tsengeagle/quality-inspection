package com.tzuchi.his.quality.inspection.acceptance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 驗收範例的執行器：依 specs/README.md 第四節的語意，逐步執行 examples.yaml 的一個範例。
 *
 * <p>判定是確定性的。範例通過就正常返回；未通過一律丟 {@link AssertionError}，訊息寫明原因。
 * 「無法執行」（needs 未滿足、輸入待補、找不到 use case）也是未通過，不是跳過。</p>
 *
 * <p>這支類別只懂規格，不認識任何 use case 的實作；呼叫一律經過 {@link UseCaseInvoker}。</p>
 */
public final class ExampleRunner {

    private static final Pattern WHOLE_VARIABLE = Pattern.compile("^\\$\\{([A-Za-z0-9_]+)}$");
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");
    /** 範例名稱是測試的身分（已知失敗清單逐行比對），只能用不會被改寫的字元。 */
    private static final Pattern EXAMPLE_NAME = Pattern.compile("^[A-Za-z0-9_.-]+$");

    private final Map<String, Object> actors;
    private final Map<String, Object> fixtures;
    private final Map<String, Map<String, Object>> examples = new LinkedHashMap<>();
    private final Set<String> specifiedUseCases;
    private final UseCaseInvoker invoker;

    /**
     * @param examplesDoc       讀進來的 examples.yaml
     * @param specifiedUseCases 所有 usecases.yaml 定義的 use case 名稱；needs 只有在這裡找得到才算滿足
     */
    public ExampleRunner(Map<String, Object> examplesDoc, Set<String> specifiedUseCases, UseCaseInvoker invoker) {
        this.actors = map(examplesDoc.get("actors"), "actors");
        this.fixtures = map(examplesDoc.get("fixtures"), "fixtures");
        this.specifiedUseCases = specifiedUseCases;
        this.invoker = invoker;
        for (Object item : list(examplesDoc.get("examples"), "examples")) {
            Map<String, Object> example = map(item, "examples 的一項");
            Object name = example.get("name");
            if (!(name instanceof String) || examples.containsKey(name)) {
                throw new RunnerProblem("範例必須有不重複的 name：" + name);
            }
            if (!EXAMPLE_NAME.matcher((String) name).matches()) {
                throw new RunnerProblem("範例名稱只能用英數字與 _ . -（它是已知失敗清單裡的身分）：" + name);
            }
            examples.put((String) name, example);
        }
    }

    public List<String> exampleNames() {
        return new ArrayList<>(examples.keySet());
    }

    /** 執行一個範例。通過則返回；未通過丟 AssertionError。 */
    public void run(String exampleName) {
        Map<String, Object> example = examples.get(exampleName);
        if (example == null) {
            throw new AssertionError("沒有這個範例：" + exampleName);
        }
        try {
            runExample(example);
        } catch (RunnerProblem problem) {
            throw new AssertionError("無法執行：" + problem.getMessage(), problem);
        }
    }

    private void runExample(Map<String, Object> example) {
        Map<String, Object> vars = new LinkedHashMap<>();

        List<Map<String, Object>> given = expand(list(example.get("given"), "given"), new LinkedHashSet<>());
        Object whenNode = example.get("when");
        if (!(whenNode instanceof Map)) {
            throw new RunnerProblem("when 必須是一個步驟");
        }
        Map<String, Object> when = map(whenNode, "when");
        List<Map<String, Object>> observe = new ArrayList<>();
        for (Object step : list(example.get("observe"), "observe")) {
            observe.add(map(step, "observe 的步驟"));
        }

        // 跳過算未通過（specs/README.md 第四節規則 4）：先檢查，不執行任何一步
        Set<String> needs = new LinkedHashSet<>(names(example.get("needs")));
        needs.addAll(fixtureNeeds(list(example.get("given"), "given"), new LinkedHashSet<>()));
        List<String> unmet = new ArrayList<>();
        for (String need : needs) {
            if (!specifiedUseCases.contains(need)) {
                unmet.add(need);
            }
        }
        if (!unmet.isEmpty()) {
            throw new AssertionError("needs 未滿足（這些 use case 還沒有規格）：" + String.join("、", unmet)
                    + "。跳過算未通過");
        }
        List<Map<String, Object>> all = new ArrayList<>(given);
        all.add(when);
        all.addAll(observe);
        for (Map<String, Object> step : all) {
            if (step.containsKey("input_pending")) {
                throw new AssertionError("輸入待補（" + step.get("run") + "）：" + step.get("input_pending")
                        + "。跳過算未通過");
            }
        }

        for (int i = 0; i < given.size(); i++) {
            Map<String, Object> step = given.get(i);
            Outcome outcome = execute(step, vars);
            if (!outcome.isSuccess()) {
                throw new AssertionError("準備步驟 given[" + i + "] " + step.get("run") + " 失敗：" + outcome);
            }
            save(step, outcome, vars);
        }

        Outcome outcome = execute(when, vars);
        checkResult("then", map(example.get("then"), "then"), outcome);
        if (outcome.isSuccess()) {
            save(when, outcome, vars);
        }

        for (int i = 0; i < observe.size(); i++) {
            Map<String, Object> step = observe.get(i);
            Outcome observed = execute(step, vars);
            Map<String, Object> expect = map(step.get("expect"), "observe[" + i + "].expect");
            String where = "observe[" + i + "] " + step.get("run");
            checkResult(where, expect, observed);
            if (expect.containsKey("output")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> expectedOutput = (Map<String, Object>) substitute(
                        map(expect.get("output"), where + " 的 expect.output"), vars);
                compareOutput(where, expectedOutput, invoker.properties(observed.getValue()));
            }
        }
    }

    // ---------- 步驟 ----------

    private List<Map<String, Object>> expand(List<Object> steps, Set<String> using) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : steps) {
            Map<String, Object> step = map(item, "步驟");
            if (step.containsKey("use")) {
                String name = String.valueOf(step.get("use"));
                Map<String, Object> fixture = map(fixtures.get(name), "fixture " + name);
                if (!using.add(name)) {
                    throw new RunnerProblem("fixture 互相引用：" + name);
                }
                out.addAll(expand(list(fixture.get("steps"), "fixture " + name + " 的 steps"), using));
                using.remove(name);
            } else {
                out.add(step);
            }
        }
        return out;
    }

    private Set<String> fixtureNeeds(List<Object> steps, Set<String> seen) {
        Set<String> out = new LinkedHashSet<>();
        for (Object item : steps) {
            Map<String, Object> step = map(item, "步驟");
            if (step.containsKey("use")) {
                String name = String.valueOf(step.get("use"));
                if (!seen.add(name)) {
                    continue;
                }
                Map<String, Object> fixture = map(fixtures.get(name), "fixture " + name);
                out.addAll(names(fixture.get("needs")));
                out.addAll(fixtureNeeds(list(fixture.get("steps"), "fixture " + name + " 的 steps"), seen));
            }
        }
        return out;
    }

    private Outcome execute(Map<String, Object> step, Map<String, Object> vars) {
        Object run = step.get("run");
        if (!(run instanceof String)) {
            throw new RunnerProblem("步驟沒有 run：" + step);
        }
        Object as = step.get("as");
        Map<String, Object> actor = map(actors.get(String.valueOf(as)), "actor " + as);
        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) substitute(map(step.get("input"), run + " 的 input"), vars);
        return invoker.invoke((String) run, actor, input);
    }

    private void save(Map<String, Object> step, Outcome outcome, Map<String, Object> vars) {
        Map<String, Object> save = map(step.get("save"), step.get("run") + " 的 save");
        for (Map.Entry<String, Object> entry : save.entrySet()) {
            String field = String.valueOf(entry.getValue());
            Object value = outcome.getValue();
            if (!isScalar(value)) {
                Map<String, Object> props = invoker.properties(value);
                if (!props.containsKey(field)) {
                    throw new AssertionError(step.get("run") + " 的輸出沒有 " + field + "，無法存成 ${" + entry.getKey() + "}");
                }
                value = props.get(field);
            }
            if (value == null) {
                throw new AssertionError(step.get("run") + " 的輸出 " + field + " 是 null，無法存成 ${" + entry.getKey() + "}");
            }
            vars.put(entry.getKey(), value);
        }
    }

    private Object substitute(Object node, Map<String, Object> vars) {
        if (node instanceof String) {
            String text = (String) node;
            Matcher whole = WHOLE_VARIABLE.matcher(text);
            if (whole.matches()) {
                return variable(whole.group(1), vars);
            }
            Matcher m = VARIABLE.matcher(text);
            StringBuffer out = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(out, Matcher.quoteReplacement(String.valueOf(variable(m.group(1), vars))));
            }
            m.appendTail(out);
            return out.toString();
        }
        if (node instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) node).entrySet()) {
                out.put(String.valueOf(e.getKey()), substitute(e.getValue(), vars));
            }
            return out;
        }
        if (node instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) node) {
                out.add(substitute(item, vars));
            }
            return out;
        }
        return node;
    }

    private static Object variable(String name, Map<String, Object> vars) {
        if (!vars.containsKey(name)) {
            throw new RunnerProblem("變數 ${" + name + "} 沒有定義；前面的步驟要先用 save 存它");
        }
        return vars.get(name);
    }

    // ---------- 判定 ----------

    private static void checkResult(String where, Map<String, Object> expected, Outcome actual) {
        Object result = expected.get("result");
        if ("SUCCESS".equals(result)) {
            if (!actual.isSuccess()) {
                throw new AssertionError(where + "：預期 SUCCESS，實際 " + actual);
            }
        } else if ("REJECTED".equals(result)) {
            Object code = expected.get("code");
            if (actual.isSuccess()) {
                throw new AssertionError(where + "：預期 REJECTED " + code + "，實際 SUCCESS");
            }
            if (actual.getCode() == null) {
                throw new AssertionError(where + "：預期 REJECTED " + code + "，實際被拒絕但沒有錯誤代碼（" + actual.getDetail() + "）");
            }
            if (!actual.getCode().equals(code)) {
                throw new AssertionError(where + "：預期 REJECTED " + code + "，實際 " + actual);
            }
        } else {
            throw new RunnerProblem(where + " 的 result 必須是 SUCCESS 或 REJECTED：" + result);
        }
    }

    private static void compareOutput(String where, Map<String, Object> expected, Map<String, Object> actual) {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            String key = entry.getKey();
            if (!actual.containsKey(key)) {
                problems.add("輸出沒有欄位 " + key);
            } else if (!same(entry.getValue(), actual.get(key))) {
                problems.add(key + " 預期 " + entry.getValue() + "，實際 " + actual.get(key));
            }
        }
        if (!problems.isEmpty()) {
            throw new AssertionError(where + "：" + String.join("；", problems));
        }
    }

    /** 部分比對的值比較：集合不比順序；數字依數值比對，規格裡寫成字串的十進位數（"0.50"）也一樣。 */
    static boolean same(Object expected, Object actual) {
        if (expected == null || actual == null) {
            return expected == null && actual == null;
        }
        if (expected instanceof List) {
            if (!(actual instanceof Collection)) {
                return false;
            }
            List<?> want = (List<?>) expected;
            List<Object> have = new ArrayList<>((Collection<?>) actual);
            if (want.size() != have.size()) {
                return false;
            }
            return matchesOneToOne(want, have);
        }
        if (expected instanceof Map) {
            if (!(actual instanceof Map)) {
                return false;
            }
            Map<?, ?> a = (Map<?, ?>) actual;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) expected).entrySet()) {
                if (!a.containsKey(e.getKey()) || !same(e.getValue(), a.get(e.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        BigDecimal ed = decimal(expected);
        BigDecimal ad = decimal(actual);
        if ((expected instanceof Number || actual instanceof Number) && ed != null && ad != null) {
            return ed.compareTo(ad) == 0;
        }
        if (expected instanceof Boolean || actual instanceof Boolean) {
            return Objects.equals(expected, actual);
        }
        return String.valueOf(expected).equals(actual instanceof Enum ? ((Enum<?>) actual).name() : String.valueOf(actual));
    }

    /**
     * 預期與實際的元素一對一配對，不看順序。用增廣路徑（可以改選先前的配對），
     * 所以部分比對的物件不會因為順序而誤判；元素數量是範例的規模，不需要更快的演算法。
     */
    private static boolean matchesOneToOne(List<?> want, List<Object> have) {
        int[] owner = new int[have.size()];
        java.util.Arrays.fill(owner, -1);
        for (int w = 0; w < want.size(); w++) {
            if (!augment(w, want, have, owner, new boolean[have.size()])) {
                return false;
            }
        }
        return true;
    }

    private static boolean augment(int w, List<?> want, List<Object> have, int[] owner, boolean[] visited) {
        for (int h = 0; h < have.size(); h++) {
            if (visited[h] || !same(want.get(w), have.get(h))) {
                continue;
            }
            visited[h] = true;
            if (owner[h] < 0 || augment(owner[h], want, have, owner, visited)) {
                owner[h] = w;
                return true;
            }
        }
        return false;
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof Number || value instanceof String) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static boolean isScalar(Object value) {
        return value == null || value instanceof CharSequence || value instanceof Number
                || value instanceof Boolean || value instanceof Enum;
    }

    // ---------- 型別 ----------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object node, String what) {
        if (node == null) {
            return Collections.emptyMap();
        }
        if (!(node instanceof Map)) {
            throw new RunnerProblem(what + " 必須是對應表：" + node);
        }
        return (Map<String, Object>) node;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object node, String what) {
        if (node == null) {
            return Collections.emptyList();
        }
        if (!(node instanceof List)) {
            throw new RunnerProblem(what + " 必須是清單：" + node);
        }
        return (List<Object>) node;
    }

    private static List<String> names(Object node) {
        List<String> out = new ArrayList<>();
        for (Object item : list(node, "needs")) {
            out.add(String.valueOf(item));
        }
        return out;
    }
}
