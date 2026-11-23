package com.tzuchi.his.quality.inspection.acceptance;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.function.BiPredicate;
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

    // ---- 以下只有 spec_version 2 使用（D-065）----
    /** 這份範例所在的規格目錄名稱。 */
    private final String home;
    /** 規格目錄名稱 → 那個目錄的 examples.yaml；跨目錄引用 fixture 時用。 */
    private final Map<String, Map<String, Object>> documents;
    private final Map<String, Object> profile;
    private final ExampleWorld world;
    private final boolean version2;
    private final Map<String, Object> expectations;

    // ---- 以下只有 http 步驟使用（D-074，格式 v2.1）----
    /** 送出請求的途徑；沒有的話 http 範例無法執行。 */
    private final HttpInvoker http;
    /** http-common.yaml：status_by_category 與 transport_codes，用來推得狀態碼。 */
    private final Map<String, Object> httpCommon;
    /** 這個規格目錄的錯誤代碼 → 分類（usecases.yaml 的 error_codes）。 */
    private final Map<String, String> errorCategories;

    /**
     * 只有一份 spec_version 1 的 examples.yaml 時用。
     *
     * @param examplesDoc       讀進來的 examples.yaml
     * @param specifiedUseCases 所有 usecases.yaml 定義的 use case 名稱；needs 只有在這裡找得到才算滿足
     */
    public ExampleRunner(Map<String, Object> examplesDoc, Set<String> specifiedUseCases, UseCaseInvoker invoker) {
        this("", Collections.singletonMap("", examplesDoc), Collections.emptyMap(), specifiedUseCases, invoker, null);
    }

    /**
     * @param home              要執行的規格目錄名稱，必須在 documents 裡
     * @param documents         規格目錄名稱 → examples.yaml。spec_version 2 的範例可以引用別的目錄的 fixture
     * @param profile           spec-profile.yaml；spec_version 2 需要 clock_start 與 local_time_zone
     * @param specifiedUseCases 所有 usecases.yaml 定義的 use case 名稱
     * @param world             spec_version 2 的時鐘、事件與埠；只有版本 1 的範例時可以是 null
     */
    public ExampleRunner(String home, Map<String, Map<String, Object>> documents, Map<String, Object> profile,
                         Set<String> specifiedUseCases, UseCaseInvoker invoker, ExampleWorld world) {
        this(home, documents, profile, specifiedUseCases, invoker, world, null, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * 規格格式 v2.1（D-074）：範例的 when 可以是 http 步驟。
     *
     * @param http            送出請求的途徑；null 表示這個環境不能跑 http 範例
     * @param httpCommon      http-common.yaml
     * @param errorCategories home 這個目錄的錯誤代碼 → 分類（usecases.yaml 的 error_codes）
     */
    public ExampleRunner(String home, Map<String, Map<String, Object>> documents, Map<String, Object> profile,
                         Set<String> specifiedUseCases, UseCaseInvoker invoker, ExampleWorld world,
                         HttpInvoker http, Map<String, Object> httpCommon, Map<String, String> errorCategories) {
        this.http = http;
        this.httpCommon = httpCommon;
        this.errorCategories = errorCategories;
        Map<String, Object> examplesDoc = documents.get(home);
        if (examplesDoc == null) {
            throw new RunnerProblem("沒有規格目錄 " + home + " 的 examples.yaml");
        }
        this.home = home;
        this.documents = documents;
        this.profile = profile;
        this.world = world;
        this.version2 = isVersion2(examplesDoc);
        this.expectations = map(examplesDoc.get("expectations"), "expectations");
        this.actors = map(examplesDoc.get("actors"), "actors");
        this.fixtures = map(examplesDoc.get("fixtures"), "fixtures");
        this.specifiedUseCases = specifiedUseCases;
        this.invoker = invoker;
        for (Object item : list(examplesDoc.get("examples"), "examples")) {
            Map<String, Object> declared = map(item, "examples 的一項");
            for (Map<String, Object> example : version2 ? expandMatrix(declared) : Collections.singletonList(declared)) {
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
    }

    private static boolean isVersion2(Map<String, Object> doc) {
        return Integer.valueOf(2).equals(doc.get("spec_version"));
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
            if (version2) {
                runExampleV2(example);
            } else {
                runExample(example);
            }
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
        return substitute(node, vars, false);
    }

    /** keys：對應表的鍵也代入變數。只有 spec_version 2 的斷言需要（路徑當鍵，條件的值可以是 ${名稱}）。 */
    private Object substitute(Object node, Map<String, Object> vars, boolean keys) {
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
                String key = String.valueOf(e.getKey());
                out.put(keys ? String.valueOf(substitute(key, vars, false)) : key, substitute(e.getValue(), vars, keys));
            }
            return out;
        }
        if (node instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) node) {
                out.add(substitute(item, vars, keys));
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
        return same(expected, actual, null);
    }

    /**
     * @param zone spec_version 2 的 local_time_zone：規格寫的是帶時區的時間時，比對時間點（D-065 第 5 點），
     *             實際值不帶時區就依這個時區解讀。null 表示 spec_version 1，時間不特別處理。
     */
    static boolean same(Object expected, Object actual, ZoneId zone) {
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
            return matchesOneToOne(want, have, zone);
        }
        if (expected instanceof Map) {
            if (!(actual instanceof Map)) {
                return false;
            }
            Map<?, ?> a = (Map<?, ?>) actual;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) expected).entrySet()) {
                if (!a.containsKey(e.getKey()) || !same(e.getValue(), a.get(e.getKey()), zone)) {
                    return false;
                }
            }
            return true;
        }
        if (zone != null) {
            Instant wanted = instantWithZone(expected);
            if (wanted != null) {
                return wanted.equals(instantOf(actual, zone));
            }
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
    private static boolean matchesOneToOne(List<?> want, List<Object> have, ZoneId zone) {
        int[] owner = new int[have.size()];
        java.util.Arrays.fill(owner, -1);
        for (int w = 0; w < want.size(); w++) {
            if (!augment(w, want, have, owner, new boolean[have.size()], zone)) {
                return false;
            }
        }
        return true;
    }

    private static boolean augment(int w, List<?> want, List<Object> have, int[] owner, boolean[] visited, ZoneId zone) {
        for (int h = 0; h < have.size(); h++) {
            if (visited[h] || !same(want.get(w), have.get(h), zone)) {
                continue;
            }
            visited[h] = true;
            if (owner[h] < 0 || augment(owner[h], want, have, owner, visited, zone)) {
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

    // ---------- spec_version 2（D-065；specs/README.md 第十節）----------

    private static final Pattern MATRIX_KEY_VALUE = Pattern.compile("^[A-Za-z0-9_-]+$");

    /** 一個步驟，連同它寫在哪個規格目錄：操作者與沒有寫目錄的 fixture 依那個目錄解讀。 */
    private static final class Step {
        final Map<String, Object> node;
        final String directory;

        Step(Map<String, Object> node, String directory) {
            this.node = node;
            this.directory = directory;
        }
    }

    /** 矩陣的每一列展開成一個範例，名稱是 {@code <name>.<key 的值，用 . 連接>}。沒有矩陣就是原來那一個。 */
    private static List<Map<String, Object>> expandMatrix(Map<String, Object> example) {
        if (!example.containsKey("matrix")) {
            return Collections.singletonList(example);
        }
        Map<String, Object> matrix = map(example.get("matrix"), "matrix");
        List<String> columns = strings(matrix.get("columns"), "matrix.columns");
        List<String> key = strings(matrix.get("key"), "matrix.key");
        if (key.isEmpty() || !columns.containsAll(key)) {
            throw new RunnerProblem("matrix.key 必填，而且必須是 columns 裡的欄：" + example.get("name"));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object row : list(matrix.get("rows"), "matrix.rows")) {
            List<Object> cells = list(row, "matrix.rows 的一列");
            if (cells.size() != columns.size()) {
                throw new RunnerProblem("matrix 的一列有 " + cells.size() + " 格，欄有 " + columns.size() + " 個：" + cells);
            }
            Map<String, Object> bindings = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                bindings.put(columns.get(i), cells.get(i));
            }
            StringBuilder name = new StringBuilder(String.valueOf(example.get("name")));
            for (String column : key) {
                String value = String.valueOf(bindings.get(column));
                if (!MATRIX_KEY_VALUE.matcher(value).matches()) {
                    throw new RunnerProblem("matrix 的 key 欄只能用英數字與 _ -：" + value);
                }
                name.append('.').append(value);
            }
            Map<String, Object> expanded = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : example.entrySet()) {
                String field = entry.getKey();
                if ("matrix".equals(field)) {
                    continue;
                }
                expanded.put(field, "name".equals(field) ? name.toString() : bind(entry.getValue(), bindings));
            }
            out.add(expanded);
        }
        return out;
    }

    /** 把 ${欄名} 換成這一列的值；不是欄名的 ${名稱} 留給執行時的變數。 */
    private static Object bind(Object node, Map<String, Object> bindings) {
        if (node instanceof String) {
            String text = (String) node;
            Matcher whole = WHOLE_VARIABLE.matcher(text);
            if (whole.matches() && bindings.containsKey(whole.group(1))) {
                return bindings.get(whole.group(1));
            }
            Matcher m = VARIABLE.matcher(text);
            StringBuffer out = new StringBuffer();
            while (m.find()) {
                String replacement = bindings.containsKey(m.group(1)) ? String.valueOf(bindings.get(m.group(1))) : m.group(0);
                m.appendReplacement(out, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(out);
            return out.toString();
        }
        if (node instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) node).entrySet()) {
                out.put(String.valueOf(bind(String.valueOf(e.getKey()), bindings)), bind(e.getValue(), bindings));
            }
            return out;
        }
        if (node instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) node) {
                out.add(bind(item, bindings));
            }
            return out;
        }
        return node;
    }

    private void runExampleV2(Map<String, Object> example) {
        if (world == null) {
            throw new RunnerProblem("spec_version 2 的範例需要時鐘、事件與埠的環境（ExampleWorld）");
        }
        Map<String, Object> vars = new LinkedHashMap<>();
        Set<String> needs = new LinkedHashSet<>(names(example.get("needs")));
        List<Step> given = expandV2(list(example.get("given"), "given"), home, new LinkedHashSet<>(), needs);
        if (!(example.get("when") instanceof Map)) {
            throw new RunnerProblem("when 必須是一個步驟");
        }
        Step when = new Step(map(example.get("when"), "when"), home);
        boolean isHttp = when.node.containsKey("http");
        if (isHttp && when.node.containsKey("run")) {
            throw new RunnerProblem("when 不能同時是 run 與 http 步驟");
        }
        if (!isHttp && !(when.node.get("run") instanceof String)) {
            throw new RunnerProblem("when 必須是 run 或 http 步驟");
        }
        for (Step step : given) {
            if (step.node.containsKey("http")) {
                throw new RunnerProblem("http 步驟只能用在 when；準備狀態一律走 use case");
            }
        }
        List<Step> observe = new ArrayList<>();
        for (Object step : list(example.get("observe"), "observe")) {
            observe.add(new Step(map(step, "observe 的步驟"), home));
            if (observe.get(observe.size() - 1).node.containsKey("http")) {
                throw new RunnerProblem("http 步驟只能用在 when，不能用在 observe");
            }
        }
        if (isHttp) {
            if (http == null) {
                throw new RunnerProblem("http 步驟需要送出請求的環境（HttpInvoker）");
            }
            // D-074 第 7 點：環境不對就停下來，不執行任何一步
            http.checkReady();
        }

        // 跳過算未通過：先檢查，不執行任何一步（與版本 1 相同）
        List<String> unmet = new ArrayList<>();
        for (String need : needs) {
            if (!specifiedUseCases.contains(useCaseName(need))) {
                unmet.add(need);
            }
        }
        if (!unmet.isEmpty()) {
            throw new AssertionError("needs 未滿足（這些 use case 還沒有規格）：" + String.join("、", unmet)
                    + "。跳過算未通過");
        }
        List<Step> all = new ArrayList<>(given);
        all.add(when);
        all.addAll(observe);
        for (Step step : all) {
            if (step.node.containsKey("input_pending")) {
                throw new AssertionError("輸入待補（" + step.node.get("run") + "）：" + step.node.get("input_pending")
                        + "。跳過算未通過");
            }
        }

        ZoneId zone = localTimeZone();
        world.begin(clockStart(example), zone);
        try {
            for (int i = 0; i < given.size(); i++) {
                prepare(given.get(i), "given[" + i + "]", vars);
            }

            world.takeEvents();
            HttpInvoker.Reply reply = isHttp ? sendHttp(when, vars) : null;
            Outcome outcome = isHttp ? httpOutcome(reply) : executeV2(when, vars);
            List<ExampleWorld.EmittedEvent> events = world.takeEvents();
            failOnUnconfiguredPorts("when");
            Map<String, Object> then = assertion(example.get("then"), "then");
            if (isHttp && (then.containsKey("status") || then.containsKey("body"))) {
                // 沒有外殼的回應（D-074 第 4 點）：只比對狀態碼與空的本文；事件與 observe 照常
                checkBareReply(then, reply);
                checkEvents(then, outcome, events, vars, zone);
                observeV2(observe, vars, zone);
                return;
            }
            if (isHttp) {
                checkHttpEnvelope(then, reply, outcome);
            }
            checkResult("then", then, outcome);
            if (isHttp) {
                checkHttpStatus(then, reply);
            }
            if (outcome.isSuccess()) {
                // 先存變數：then 的 output 與事件可以引用 when 自己存的變數
                saveV2(when, outcome, vars);
            }
            checkAssertion("then", then, outcome, vars, zone);
            checkEvents(then, outcome, events, vars, zone);
            observeV2(observe, vars, zone);
        } finally {
            world.end();
        }
    }

    private void observeV2(List<Step> observe, Map<String, Object> vars, ZoneId zone) {
        for (int i = 0; i < observe.size(); i++) {
            Step step = observe.get(i);
            if (!step.node.containsKey("run")) {
                prepare(step, "observe[" + i + "]", vars);
                continue;
            }
            String where = "observe[" + i + "] " + step.node.get("run");
            Outcome observed = executeV2(step, vars);
            failOnUnconfiguredPorts(where);
            Map<String, Object> expect = assertion(step.node.get("expect"), where + " 的 expect");
            if (expect.containsKey("emits") || expect.containsKey("emits_include")) {
                throw new RunnerProblem(where + "：emits、emits_include 只能用在 then");
            }
            checkResult(where, expect, observed);
            checkAssertion(where, expect, observed, vars, zone);
        }
    }

    // ---------- http 步驟（D-074；specs/README.md 第十一節）----------

    private static final String ANONYMOUS = "anonymous";

    /** 依 http 步驟組出請求並送出。操作者的角色經 spec-profile.yaml 的 role_claims 轉成 api-key 的 role_name。 */
    private HttpInvoker.Reply sendHttp(Step step, Map<String, Object> vars) {
        Map<String, Object> request = map(step.node.get("http"), "http");
        for (String key : new String[] {"input", "save", "expect"}) {
            if (step.node.containsKey(key)) {
                throw new RunnerProblem("http 步驟不能有 " + key);
            }
        }
        Object method = request.get("method");
        Object path = request.get("path");
        if (!(method instanceof String) || !(path instanceof String) || !((String) path).startsWith("/")) {
            throw new RunnerProblem("http 步驟必須有 method 與以 / 開頭的 path：" + request);
        }
        if (request.containsKey("body") && request.containsKey("body_raw")) {
            throw new RunnerProblem("http.body 與 http.body_raw 只能擇一");
        }
        Object raw = request.get("body_raw");
        if (request.containsKey("body_raw") && !(raw instanceof String)) {
            throw new RunnerProblem("http.body_raw 必須是字串：" + raw);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> query = (Map<String, Object>) substitute(map(request.get("query"), "http.query"), vars);
        Object body = request.containsKey("body") ? substitute(map(request.get("body"), "http.body"), vars) : null;
        String target = String.valueOf(substitute(path, vars));

        Object as = step.node.get("as");
        if (!(as instanceof String)) {
            throw new RunnerProblem("http 步驟必須有 as（不帶 api-key 時寫 as: " + ANONYMOUS + "）");
        }
        if (ANONYMOUS.equals(as)) {
            return http.send(new HttpInvoker.Request((String) method, target, query, body, (String) raw,
                    true, null, null, null));
        }
        Map<String, Object> declared = map(documents.get(step.directory).get("actors"), step.directory + " 的 actors");
        Map<String, Object> actor = map(declared.get(as), "actor " + as);
        if (actor.isEmpty()) {
            throw new RunnerProblem("actors 裡沒有操作者 " + as);
        }
        Map<String, Object> claims = map(profile.get("role_claims"), "spec-profile.yaml 的 role_claims");
        List<String> roleClaims = new ArrayList<>();
        for (String role : strings(actor.get("roles"), "actor " + as + " 的 roles")) {
            if (!(claims.get(role) instanceof String)) {
                throw new RunnerProblem("spec-profile.yaml 的 role_claims 沒有角色 " + role);
            }
            roleClaims.add((String) claims.get(role));
        }
        Object hospital = actor.get("hospital");
        return http.send(new HttpInvoker.Request((String) method, target, query, body, (String) raw,
                false, String.valueOf(actor.get("user")), hospital == null ? null : String.valueOf(hospital), roleClaims));
    }

    /** 回應是 2xx 就是成功，值是外殼的 data；其他一律是被拒絕，代碼取自外殼的 error.code（D-074 第 8 點）。 */
    private static Outcome httpOutcome(HttpInvoker.Reply reply) {
        Object json = reply.json;
        if (reply.status >= 200 && reply.status < 300) {
            return Outcome.success(json instanceof Map ? ((Map<?, ?>) json).get("data") : null);
        }
        Object error = json instanceof Map ? ((Map<?, ?>) json).get("error") : null;
        Object code = error instanceof Map ? ((Map<?, ?>) error).get("code") : null;
        return Outcome.rejected(code == null ? null : String.valueOf(code), "HTTP " + reply.status + " " + reply.body);
    }

    /** then: { status, body: empty }：沒有外殼的回應。 */
    private static void checkBareReply(Map<String, Object> then, HttpInvoker.Reply reply) {
        if (!(then.get("status") instanceof Integer) || !"empty".equals(then.get("body")) || then.size() != 2) {
            throw new RunnerProblem("沒有回應外殼時，then 只能寫 status（整數）與 body: empty：" + then);
        }
        if (reply.status != (Integer) then.get("status")) {
            throw new AssertionError("then：預期狀態碼 " + then.get("status") + "，實際 " + reply.status + "（" + reply.body + "）");
        }
        if (!reply.body.isEmpty()) {
            throw new AssertionError("then：預期回應沒有本文，實際是 " + reply.body);
        }
    }

    /** 寫 result 的範例預期回應有外殼：成功時要讀得到 data，才不會把沒有本文的 2xx 當成「data 是 null」。 */
    private static void checkHttpEnvelope(Map<String, Object> then, HttpInvoker.Reply reply, Outcome outcome) {
        if ("SUCCESS".equals(then.get("result")) && outcome.isSuccess() && !(reply.json instanceof Map)) {
            throw new AssertionError("then：回應是 " + reply.status + "，但本文不是回應外殼：" + reply.body);
        }
    }

    /**
     * 狀態碼不寫在範例裡，依 http-common.yaml 推得（D-074 第 4 點）：transport_codes 列出的代碼用它的狀態碼，
     * 其他依錯誤代碼的分類查 status_by_category。在 checkResult 之後呼叫，所以代碼已經相符。
     */
    private void checkHttpStatus(Map<String, Object> then, HttpInvoker.Reply reply) {
        if (!"REJECTED".equals(then.get("result"))) {
            return;
        }
        String code = String.valueOf(then.get("code"));
        Integer expected = null;
        String reason = null;
        for (Object item : list(httpCommon.get("transport_codes"), "http-common.yaml 的 transport_codes")) {
            Map<String, Object> entry = map(item, "transport_codes 的一項");
            if (code.equals(entry.get("code")) && entry.get("status") instanceof Integer) {
                expected = (Integer) entry.get("status");
                reason = "http-common.yaml 的 transport_codes";
            }
        }
        String category = errorCategories.get(code);
        if (category != null) {
            Object byCategory = map(httpCommon.get("status_by_category"), "http-common.yaml 的 status_by_category").get(category);
            if (!(byCategory instanceof Integer)) {
                throw new RunnerProblem("http-common.yaml 的 status_by_category 沒有分類 " + category + "（錯誤代碼 " + code + "）");
            }
            if (expected != null && !expected.equals(byCategory)) {
                throw new RunnerProblem("錯誤代碼 " + code + " 的狀態碼有兩個說法：transport_codes 是 " + expected
                        + "，分類 " + category + " 是 " + byCategory);
            }
            if (expected == null) {
                expected = (Integer) byCategory;
                reason = "分類 " + category;
            }
        }
        if (expected == null) {
            throw new RunnerProblem("推不出錯誤代碼 " + code + " 的狀態碼：它不在 " + home
                    + " 的 error_codes，也不在 http-common.yaml 的 transport_codes");
        }
        if (reply.status != expected) {
            throw new AssertionError("then：" + code + " 的狀態碼應為 " + expected + "（" + reason + "），實際 " + reply.status);
        }
    }

    /** 展開 use（可以是 {@code <目錄>.<名稱>}），並收集 fixture 的 needs。 */
    private List<Step> expandV2(List<Object> steps, String directory, Set<String> using, Set<String> needs) {
        List<Step> out = new ArrayList<>();
        for (Object item : steps) {
            Map<String, Object> step = map(item, "步驟");
            if (!step.containsKey("use")) {
                out.add(new Step(step, directory));
                continue;
            }
            String reference = String.valueOf(step.get("use"));
            int dot = reference.indexOf('.');
            String target = dot > 0 ? reference.substring(0, dot) : directory;
            String name = dot > 0 ? reference.substring(dot + 1) : reference;
            Map<String, Object> document = documents.get(target);
            if (document == null) {
                throw new RunnerProblem("fixture " + reference + "：沒有規格目錄 " + target + " 的 examples.yaml");
            }
            Map<String, Object> declared = map(document.get("fixtures"), target + " 的 fixtures");
            if (!declared.containsKey(name)) {
                throw new RunnerProblem("沒有 fixture " + reference);
            }
            Map<String, Object> fixture = map(declared.get(name), "fixture " + reference);
            String id = target + "." + name;
            if (!using.add(id)) {
                throw new RunnerProblem("fixture 互相引用：" + reference);
            }
            needs.addAll(names(fixture.get("needs")));
            out.addAll(expandV2(list(fixture.get("steps"), "fixture " + reference + " 的 steps"), target, using, needs));
            using.remove(id);
        }
        return out;
    }

    /** 準備步驟：run（必須成功）、port（設定回傳值）、advance（時間前進）。 */
    private void prepare(Step step, String where, Map<String, Object> vars) {
        Map<String, Object> node = step.node;
        if (node.containsKey("advance")) {
            Duration amount;
            try {
                amount = Duration.parse(String.valueOf(node.get("advance")));
            } catch (DateTimeParseException e) {
                throw new RunnerProblem(where + "：advance 必須是 ISO 8601 期間（例如 PT1H）：" + node.get("advance"));
            }
            if (amount.isZero() || amount.isNegative()) {
                throw new RunnerProblem(where + "：advance 只能前進：" + node.get("advance"));
            }
            world.advance(amount);
        } else if (node.containsKey("port")) {
            String[] parts = String.valueOf(node.get("port")).split("\\.");
            if (parts.length < 2 || parts.length > 3) {
                throw new RunnerProblem(where + "：port 必須是 <埠>.<操作> 或 <目錄>.<埠>.<操作>：" + node.get("port"));
            }
            if (!node.containsKey("returns")) {
                throw new RunnerProblem(where + "：port 步驟必須有 returns：" + node.get("port"));
            }
            world.portReturns(parts[parts.length - 2], parts[parts.length - 1], substitute(node.get("returns"), vars));
        } else {
            Outcome outcome = executeV2(step, vars);
            failOnUnconfiguredPorts(where + " " + node.get("run"));
            if (!outcome.isSuccess()) {
                throw new AssertionError("準備步驟 " + where + " " + node.get("run") + " 失敗：" + outcome);
            }
            saveV2(step, outcome, vars);
        }
    }

    private Outcome executeV2(Step step, Map<String, Object> vars) {
        Object run = step.node.get("run");
        if (!(run instanceof String)) {
            throw new RunnerProblem("步驟必須是 run、use、port、advance 其中一種：" + step.node);
        }
        Map<String, Object> declared = map(documents.get(step.directory).get("actors"), step.directory + " 的 actors");
        Object as = step.node.get("as");
        Map<String, Object> actor = map(declared.get(String.valueOf(as)), "actor " + as);
        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) substitute(map(step.node.get("input"), run + " 的 input"), vars);
        return invoker.invoke(useCaseName((String) run), actor, input);
    }

    /** {@code <目錄>.<名稱>} 的名稱。執行器依名稱找類別，目錄只給檢查程式用。 */
    private static String useCaseName(String reference) {
        return reference.substring(reference.lastIndexOf('.') + 1);
    }

    private void failOnUnconfiguredPorts(String where) {
        List<String> calls = world.takeUnconfiguredPortCalls();
        if (!calls.isEmpty()) {
            throw new AssertionError(where + "：執行到沒有設定 returns 的埠：" + String.join("、", calls));
        }
    }

    private void saveV2(Step step, Outcome outcome, Map<String, Object> vars) {
        Map<String, Object> save = map(step.node.get("save"), step.node.get("run") + " 的 save");
        for (Map.Entry<String, Object> entry : save.entrySet()) {
            String path = String.valueOf(entry.getValue());
            Object value = outcome.getValue();
            if (!isScalar(value)) {
                TreePath parsed = TreePath.parse(path);
                if (parsed.selectsMany()) {
                    throw new RunnerProblem(step.node.get("run") + " 的 save 不能用 [*]、[k=v]：" + path);
                }
                List<Object> selected = parsed.select(world.tree(value), (a, b) -> false);
                if (selected.isEmpty() || selected.get(0) == TreePath.MISSING) {
                    throw new AssertionError(step.node.get("run") + " 的輸出沒有 " + path + "，無法存成 ${" + entry.getKey() + "}");
                }
                value = selected.get(0);
            }
            if (value == null) {
                throw new AssertionError(step.node.get("run") + " 的輸出 " + path + " 是 null，無法存成 ${" + entry.getKey() + "}");
            }
            vars.put(entry.getKey(), value);
        }
    }

    /** 展開 use_expect：具名斷言在下，直接寫的欄位蓋上去（與 spec_lint 相同）。 */
    private Map<String, Object> assertion(Object node, String where) {
        Map<String, Object> written = map(node, where);
        if (!written.containsKey("use_expect")) {
            return written;
        }
        String name = String.valueOf(written.get("use_expect"));
        if (!expectations.containsKey(name)) {
            throw new RunnerProblem(where + "：use_expect 引用了不存在的具名斷言 " + name);
        }
        Map<String, Object> base = map(expectations.get(name), "具名斷言 " + name);
        if (base.containsKey("use_expect")) {
            throw new RunnerProblem("具名斷言不能再引用具名斷言：" + name);
        }
        Map<String, Object> merged = new LinkedHashMap<>(base);
        merged.putAll(written);
        merged.remove("use_expect");
        return merged;
    }

    /** output、absent、only。result 與 code 已經由 checkResult 判定。 */
    private void checkAssertion(String where, Map<String, Object> assertion, Outcome outcome,
                                Map<String, Object> vars, ZoneId zone) {
        boolean hasOutputChecks = assertion.containsKey("output") || assertion.containsKey("absent")
                || assertion.containsKey("only");
        if ("REJECTED".equals(assertion.get("result"))) {
            if (hasOutputChecks) {
                throw new RunnerProblem(where + "：REJECTED 只能有 code");
            }
            return;
        }
        if (!hasOutputChecks) {
            return;
        }
        Object tree = world.tree(outcome.getValue());
        BiPredicate<Object, Object> equal = (expected, actual) -> same(expected, actual, zone);
        List<String> problems = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) substitute(map(assertion.get("output"), where + " 的 output"), vars, true);
        for (Map.Entry<String, Object> entry : output.entrySet()) {
            String key = entry.getKey();
            if (!TreePath.isPath(key)) {
                // 單純的屬性名稱：與版本 1 相同
                if (!(tree instanceof Map) || !((Map<?, ?>) tree).containsKey(key)) {
                    problems.add("輸出沒有欄位 " + key);
                } else if (!same(entry.getValue(), ((Map<?, ?>) tree).get(key), zone)) {
                    problems.add(key + " 預期 " + entry.getValue() + "，實際 " + ((Map<?, ?>) tree).get(key));
                }
                continue;
            }
            TreePath path = outputPath(where, key);
            List<Object> selected = path.select(tree, equal);
            if (selected.isEmpty()) {
                problems.add(key + " 選不到任何一筆");
            }
            for (Object value : selected) {
                if (value == TreePath.MISSING) {
                    problems.add("輸出沒有 " + key);
                } else if (!same(entry.getValue(), value, zone)) {
                    problems.add(key + " 預期 " + entry.getValue() + "，實際 " + value);
                }
            }
        }

        for (Object item : list(substitute(assertion.get("absent"), vars), where + " 的 absent")) {
            String key = String.valueOf(item);
            TreePath path = outputPath(where, key);
            List<Object> selected = path.select(tree, equal);
            if (selected.isEmpty()) {
                problems.add("absent " + key + " 選不到任何一筆");
            }
            for (Object value : selected) {
                if (value != TreePath.MISSING && value != null) {
                    problems.add(key + " 應該不存在或為 null，實際 " + value);
                }
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> only = (Map<String, Object>) substitute(map(assertion.get("only"), where + " 的 only"), vars, true);
        for (Map.Entry<String, Object> entry : only.entrySet()) {
            String key = entry.getKey();
            List<String> allowed = strings(entry.getValue(), where + " 的 only." + key);
            List<Object> selected = outputPath(where, key).select(tree, equal);
            if (selected.isEmpty()) {
                problems.add("only " + key + " 選不到任何一筆");
            }
            for (Object value : selected) {
                if (!(value instanceof Map)) {
                    problems.add("only " + key + " 選到的不是物件：" + value);
                    continue;
                }
                for (Map.Entry<?, ?> property : ((Map<?, ?>) value).entrySet()) {
                    if (property.getValue() != null && !allowed.contains(String.valueOf(property.getKey()))) {
                        problems.add(key + " 有清單以外的屬性 " + property.getKey() + "=" + property.getValue());
                    }
                }
            }
        }

        if (!problems.isEmpty()) {
            throw new AssertionError(where + "：" + String.join("；", problems));
        }
    }

    private static TreePath outputPath(String where, String text) {
        TreePath path = TreePath.parse(text);
        if (path.usesIndex()) {
            throw new RunnerProblem(where + "：a[0] 只能用在 save：" + text);
        }
        return path;
    }

    /** 事件（D-065 第 7 點）。被拒絕時一律不得有事件；emits 是完整的清單，emits_include 是部分比對。 */
    private void checkEvents(Map<String, Object> then, Outcome outcome, List<ExampleWorld.EmittedEvent> events,
                             Map<String, Object> vars, ZoneId zone) {
        List<Object> have = new ArrayList<>();
        for (ExampleWorld.EmittedEvent event : events) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("event", event.getName());
            item.put("with", event.getProperties());
            have.add(item);
        }
        if (!outcome.isSuccess()) {
            if (!have.isEmpty()) {
                throw new AssertionError("then：被拒絕卻發出了事件：" + events);
            }
            return;
        }
        if ("REJECTED".equals(then.get("result"))) {
            return;
        }
        if (then.containsKey("emits")) {
            List<Object> want = wantedEvents(then.get("emits"), "then.emits", vars);
            if (want.size() != have.size() || !matchesOneToOne(want, have, zone)) {
                throw new AssertionError("then.emits：預期剛好發出 " + want + "，實際 " + have);
            }
        }
        if (then.containsKey("emits_include")) {
            List<Object> want = wantedEvents(then.get("emits_include"), "then.emits_include", vars);
            if (!matchesOneToOne(want, have, zone)) {
                throw new AssertionError("then.emits_include：預期發出的事件包含 " + want + "，實際 " + have);
            }
        }
    }

    private List<Object> wantedEvents(Object node, String where, Map<String, Object> vars) {
        List<Object> out = new ArrayList<>();
        for (Object item : list(substitute(node, vars), where)) {
            Map<String, Object> declared = map(item, where + " 的一項");
            if (!(declared.get("event") instanceof String)) {
                throw new RunnerProblem(where + " 的每一項必須有 event：" + item);
            }
            Map<String, Object> wanted = new LinkedHashMap<>();
            wanted.put("event", declared.get("event"));
            wanted.put("with", map(declared.get("with"), where + " 的 with"));
            out.add(wanted);
        }
        return out;
    }

    private ZoneId localTimeZone() {
        Object zone = profile.get("local_time_zone");
        if (!(zone instanceof String)) {
            throw new RunnerProblem("spec-profile.yaml 沒有 local_time_zone；spec_version 2 的規格需要 profile_version 2");
        }
        try {
            return ZoneId.of((String) zone);
        } catch (DateTimeException e) {
            throw new RunnerProblem("spec-profile.yaml 的 local_time_zone 不是時區：" + zone);
        }
    }

    private Instant clockStart(Map<String, Object> example) {
        Map<String, Object> clock = map(example.get("clock"), "clock");
        Object start = clock.containsKey("start") ? clock.get("start") : profile.get("clock_start");
        Instant instant = instantWithZone(start);
        if (instant == null) {
            throw new RunnerProblem("時鐘的起始時間必須是帶時區的 ISO 8601 時間（範例的 clock.start 或 spec-profile.yaml 的 clock_start）：" + start);
        }
        return instant;
    }

    /** 規格裡帶時區的時間；不是的話回傳 null。YAML 沒加引號的時間會被讀成 Date。 */
    private static Instant instantWithZone(Object value) {
        if (value instanceof Date) {
            return ((Date) value).toInstant();
        }
        if (value instanceof String) {
            try {
                return OffsetDateTime.parse((String) value).toInstant();
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }

    /** 實際值代表的時間點；不帶時區的時間依 zone 解讀。不是時間就回傳 null。 */
    private static Instant instantOf(Object actual, ZoneId zone) {
        if (actual instanceof Instant) {
            return (Instant) actual;
        }
        if (actual instanceof OffsetDateTime) {
            return ((OffsetDateTime) actual).toInstant();
        }
        if (actual instanceof ZonedDateTime) {
            return ((ZonedDateTime) actual).toInstant();
        }
        if (actual instanceof LocalDateTime) {
            return ((LocalDateTime) actual).atZone(zone).toInstant();
        }
        if (actual instanceof Date) {
            return ((Date) actual).toInstant();
        }
        if (actual instanceof String) {
            Instant withZone = instantWithZone(actual);
            if (withZone != null) {
                return withZone;
            }
            try {
                return LocalDateTime.parse((String) actual).atZone(zone).toInstant();
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }

    private static List<String> strings(Object node, String what) {
        List<String> out = new ArrayList<>();
        for (Object item : list(node, what)) {
            out.add(String.valueOf(item));
        }
        return out;
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
