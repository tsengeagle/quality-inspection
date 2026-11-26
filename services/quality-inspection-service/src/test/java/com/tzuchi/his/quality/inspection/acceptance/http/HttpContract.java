package com.tzuchi.his.quality.inspection.acceptance.http;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HTTP 契約（docs/harness/13-http-contract-options.md 第八節）。
 *
 * <ul>
 *   <li>共用層 {@code http-common.yaml}：回應外殼、錯誤分類對應的狀態碼、不屬於任何 use case 的錯誤代碼。</li>
 *   <li>aggregate 層 {@code <aggregate>/http.yaml}：端點與回應的 data 形狀。每個端點屬於同一個規格目錄裡的一支 use case。</li>
 * </ul>
 *
 * <p>錯誤代碼與分類讀同一個規格目錄的 {@code usecases.yaml}，列舉讀 {@code model.yaml}；
 * 草稿目錄沒有這兩個檔時，讀 {@code specs/} 底下同名目錄的。</p>
 *
 * <p>載入時發現的問題放在 {@link #loadProblems()}，不丟例外：有問題的契約仍要回報，不能因此變成沒有評估。</p>
 */
public final class HttpContract {

    /** 端點成功回應的一種 data 形狀（D-088：一個端點可以有幾種形狀，每筆回應必須剛好符合其中一種）。 */
    public static final class Variant {
        private final String name;
        private final Object shape;
        private final List<String> arrays;
        private final List<String> nullables;
        private final String transition;

        Variant(String name, Object shape, List<String> arrays, List<String> nullables) {
            this(name, shape, arrays, nullables, null);
        }

        Variant(String name, Object shape, List<String> arrays, List<String> nullables, String transition) {
            this.transition = transition;
            this.name = name;
            this.shape = shape;
            this.arrays = arrays;
            this.nullables = nullables;
        }

        /**
         * D-094：形狀在過渡期間時，是推動過渡的裁決編號（例如 {@code D-094}），否則是 null。
         * 過渡中的形狀不檢查涵蓋（修法 PR 與建構 PR 之間，新舊形狀各有一段時間觀察不到），
         * 但每筆回應仍要剛好符合其中一種形狀。報告列出過渡中的形狀，表示它們的涵蓋沒有被評估。
         */
        public String transition() { return transition; }

        /** 形狀的名稱；端點只有 {@code data}、沒有 {@code variants} 時是 null。 */
        public String name() { return name; }
        Object shape() { return shape; }
        /** 這種形狀裡的陣列位置，以 {@code $.data} 開頭，巢狀的索引寫成 []。 */
        List<String> arrays() { return arrays; }
        /** 這種形狀裡可為 null 的欄位位置，寫法同 {@link #arrays()}。 */
        List<String> nullables() { return nullables; }
    }

    /** 一個端點。 */
    public static final class Endpoint {
        private final String aggregate;
        private final String usecase;
        private final String method;
        private final String path;
        private final String[] segments;
        private final int successStatus;
        private final List<Variant> variants;

        Endpoint(String aggregate, String usecase, String method, String path, int successStatus,
                 List<Variant> variants) {
            this.aggregate = aggregate;
            this.usecase = usecase;
            this.method = method;
            this.path = path;
            this.segments = split(path);
            this.successStatus = successStatus;
            this.variants = variants;
        }

        public String aggregate() { return aggregate; }
        public String usecase() { return usecase; }
        public String method() { return method; }
        public String path() { return path; }
        public int successStatus() { return successStatus; }
        /** 成功回應的 data 形狀，至少一種。 */
        List<Variant> variants() { return variants; }

        public String key() { return method + " " + path; }

        /** 對到時回傳字面段的數量（越大越具體），對不到回傳 -1。 */
        int score(String method, String[] actual) {
            if (!this.method.equals(method) || actual.length != segments.length) return -1;
            int literal = 0;
            for (int i = 0; i < segments.length; i++) {
                if (isVariable(segments[i])) {
                    if (actual[i].isEmpty()) return -1;
                } else if (segments[i].equals(actual[i])) {
                    literal++;
                } else {
                    return -1;
                }
            }
            return literal;
        }
    }

    private final List<String> loadProblems = new ArrayList<>();
    private final List<Endpoint> endpoints = new ArrayList<>();
    private final Map<String, Integer> statusByCategory = new HashMap<>();
    private final Map<String, Integer> transportCodes = new TreeMap<>();
    private final Map<String, Map<String, String>> codeCategories = new HashMap<>();   // aggregate -> code -> category
    private final Map<String, Map<String, Set<String>>> enums = new HashMap<>();       // aggregate -> enum -> values
    private String basePath = "";
    private Object successEnvelope;
    private Object errorEnvelope;

    private HttpContract() {}

    /** 從目錄載入。contractRoot 底下每個有 http.yaml 的子目錄是一個 aggregate。 */
    public static HttpContract load(Path commonFile, Path contractRoot, Path specsDir) {
        HttpContract c = new HttpContract();
        String common = c.read(commonFile);
        Map<String, String> http = new TreeMap<>();
        Map<String, String> usecases = new HashMap<>();
        Map<String, String> models = new HashMap<>();
        if (Files.isDirectory(contractRoot)) {
            try (Stream<Path> dirs = Files.list(contractRoot)) {
                for (Path dir : dirs.filter(d -> Files.isRegularFile(d.resolve("http.yaml"))).sorted().collect(Collectors.toList())) {
                    String name = dir.getFileName().toString();
                    http.put(name, c.read(dir.resolve("http.yaml")));
                    usecases.put(name, c.readFirst(dir.resolve("usecases.yaml"), specsDir.resolve(name).resolve("usecases.yaml")));
                    models.put(name, c.readFirst(dir.resolve("model.yaml"), specsDir.resolve(name).resolve("model.yaml")));
                }
            } catch (IOException e) {
                c.loadProblems.add(contractRoot + "：讀不到目錄（" + e.getMessage() + "）");
            }
        }
        if (http.isEmpty()) {
            c.loadProblems.add(contractRoot + "：找不到任何 http.yaml；契約沒有被評估");
        }
        c.build(common, http, usecases, models);
        return c;
    }

    /** 從字串建立（測試用）。四個對應表都以 aggregate 目錄名稱為鍵；usecases、models 的值可以是 null。 */
    public static HttpContract parse(String common, Map<String, String> http, Map<String, String> usecases,
                                     Map<String, String> models) {
        HttpContract c = new HttpContract();
        c.build(common, new TreeMap<>(http), usecases, models);
        return c;
    }

    public List<String> loadProblems() { return Collections.unmodifiableList(loadProblems); }
    public List<Endpoint> endpoints() { return Collections.unmodifiableList(endpoints); }
    Object successEnvelope() { return successEnvelope; }
    Object errorEnvelope() { return errorEnvelope; }
    Map<String, Set<String>> enumsOf(String aggregate) { return enums.getOrDefault(aggregate, Map.of()); }

    /** 錯誤代碼應有的狀態碼；說明放在 reason[0]。沒有定義時回傳 null。 */
    Integer expectedStatus(Endpoint endpoint, String code, String[] reason) {
        if (transportCodes.containsKey(code)) {
            reason[0] = code + " 的狀態碼應為 " + transportCodes.get(code);
            return transportCodes.get(code);
        }
        String category = codeCategories.getOrDefault(endpoint.aggregate(), Map.of()).get(code);
        if (category == null) return null;
        Integer status = statusByCategory.get(category);
        reason[0] = code + " 的分類是 " + category + "，狀態碼應為 " + status;
        return status;
    }

    public Optional<Endpoint> match(String method, String uri) {
        String path = uri;
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        if (!basePath.isEmpty() && (path.equals(basePath) || path.startsWith(basePath + "/"))) {
            path = path.substring(basePath.length());
        }
        String[] actual = split(path);
        Endpoint best = null;
        int bestScore = -1;
        for (Endpoint e : endpoints) {
            int s = e.score(method, actual);
            if (s > bestScore) {
                best = e;
                bestScore = s;
            }
        }
        return Optional.ofNullable(best);
    }

    // ---------- 建立 ----------

    private void build(String common, Map<String, String> http, Map<String, String> usecases, Map<String, String> models) {
        Map<?, ?> c = yaml("http-common.yaml", common, "http-common");
        if (c != null) {
            if (c.get("base_path") instanceof String) basePath = (String) c.get("base_path");
            Map<?, ?> envelope = map(c.get("envelope"));
            successEnvelope = envelope.get("success");
            errorEnvelope = envelope.get("error");
            if (successEnvelope == null || errorEnvelope == null) {
                loadProblems.add("http-common.yaml：envelope 必須有 success 與 error");
            } else {
                List<String> ignored = new ArrayList<>();
                JsonShape.validate(successEnvelope, "http-common.yaml envelope.success", Map.of(), true, loadProblems, ignored, new ArrayList<>());
                JsonShape.validate(errorEnvelope, "http-common.yaml envelope.error", Map.of(), false, loadProblems, ignored, new ArrayList<>());
            }
            for (Map.Entry<?, ?> e : map(c.get("status_by_category")).entrySet()) {
                if (e.getValue() instanceof Integer) statusByCategory.put(String.valueOf(e.getKey()), (Integer) e.getValue());
                else loadProblems.add("http-common.yaml：status_by_category." + e.getKey() + " 必須是整數");
            }
            for (Object t : list(c.get("transport_codes"))) {
                Map<?, ?> m = map(t);
                if (m.get("code") instanceof String && m.get("status") instanceof Integer) {
                    transportCodes.put((String) m.get("code"), (Integer) m.get("status"));
                } else {
                    loadProblems.add("http-common.yaml：transport_codes 每一項要有 code 與整數 status：" + t);
                }
            }
        }

        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, String> a : http.entrySet()) {
            String aggregate = a.getKey();
            Set<String> names = new HashSet<>();
            Map<String, String> categories = new LinkedHashMap<>();
            Map<?, ?> u = yaml(aggregate + "/usecases.yaml", usecases.get(aggregate), "usecases");
            if (u != null) {
                for (Object x : list(u.get("usecases"))) {
                    Object n = map(x).get("name");
                    if (n != null) names.add(String.valueOf(n));
                }
                for (Object x : list(u.get("error_codes"))) {
                    Map<?, ?> m = map(x);
                    String category = String.valueOf(m.get("category"));
                    if (!statusByCategory.containsKey(category)) {
                        loadProblems.add(aggregate + "/usecases.yaml：錯誤代碼 " + m.get("code") + " 的分類 " + category
                                + " 在 http-common.yaml 沒有對應的狀態碼");
                    }
                    categories.put(String.valueOf(m.get("code")), category);
                }
            }
            codeCategories.put(aggregate, categories);
            Map<String, Set<String>> en = new HashMap<>();
            Map<?, ?> model = yaml(aggregate + "/model.yaml", models.get(aggregate), "aggregate");
            if (model != null) {
                for (Object x : list(model.get("enums"))) {
                    Map<?, ?> m = map(x);
                    en.put(String.valueOf(m.get("name")),
                            list(m.get("values")).stream().map(String::valueOf).collect(Collectors.toSet()));
                }
            }
            enums.put(aggregate, en);

            Map<?, ?> h = yaml(aggregate + "/http.yaml", a.getValue(), "http");
            if (h == null) continue;
            for (Object x : list(h.get("endpoints"))) {
                Map<?, ?> m = map(x);
                String usecase = String.valueOf(m.get("usecase"));
                String method = String.valueOf(m.get("method"));
                String path = String.valueOf(m.get("path"));
                String where = aggregate + "/http.yaml " + method + " " + path;
                if (!names.contains(usecase)) {
                    loadProblems.add(where + "：use case " + usecase + " 不在 " + aggregate
                            + " 的 usecases.yaml（端點屬於它呼叫的 use case 所在的規格目錄）");
                }
                if (!path.startsWith("/")) {
                    loadProblems.add(where + "：路徑必須以 / 開頭");
                }
                String normalized = method + " " + path.replaceAll("\\{[^/]*}", "{}");
                if (!keys.add(normalized)) {
                    loadProblems.add(where + "：重複的端點 " + normalized);
                }
                Map<?, ?> success = map(m.get("success"));
                if (!(success.get("status") instanceof Integer) || success.containsKey("data") == success.containsKey("variants")) {
                    loadProblems.add(where + "：success 必須有整數 status，以及 data 或 variants 其中一個");
                    continue;
                }
                List<Variant> variants = new ArrayList<>();
                if (success.containsKey("data")) {
                    variants.add(variant(null, success.get("data"), where, en));
                } else {
                    List<?> raw = list(success.get("variants"));
                    if (raw.size() < 2) {
                        loadProblems.add(where + "：variants 至少要有兩種形狀（只有一種時用 data）");
                    }
                    Set<String> variantNames = new HashSet<>();
                    for (Object v : raw) {
                        Map<?, ?> vm = map(v);
                        Object name = vm.get("name");
                        Object transition = vm.get("transition");
                        int expected = vm.containsKey("transition") ? 3 : 2;
                        if (!(name instanceof String) || !vm.containsKey("data") || vm.size() != expected) {
                            loadProblems.add(where + "：variants 的每一項必須剛好有 name（字串）與 data，可以另有 transition");
                            continue;
                        }
                        if (vm.containsKey("transition")
                                && !(transition instanceof String && ((String) transition).matches("D-\\d{3}"))) {
                            loadProblems.add(where + "（" + name + "）：transition 必須是裁決編號（例如 D-094）");
                            transition = null;
                        }
                        if (!variantNames.add((String) name)) {
                            loadProblems.add(where + "：重複的形狀名稱 " + name);
                        }
                        variants.add(variant((String) name, vm.get("data"), where + "（" + name + "）", en,
                                transition instanceof String ? (String) transition : null));
                    }
                    if (variants.isEmpty()) continue;
                }
                endpoints.add(new Endpoint(aggregate, usecase, method, path, (Integer) success.get("status"), variants));
            }
        }
    }

    private Variant variant(String name, Object shape, String where, Map<String, Set<String>> en) {
        return variant(name, shape, where, en, null);
    }

    private Variant variant(String name, Object shape, String where, Map<String, Set<String>> en, String transition) {
        List<String> arrays = new ArrayList<>();
        List<String> nullables = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        JsonShape.validate(shape, "$.data", en, false, problems, arrays, nullables);
        if (name != null) problems.forEach(p -> loadProblems.add(where + " " + p));
        else loadProblems.addAll(problems);
        return new Variant(name, shape, arrays, nullables, transition);
    }

    private Map<?, ?> yaml(String name, String text, String kind) {
        if (text == null) {
            if (!"aggregate".equals(kind)) loadProblems.add(name + "：找不到這個檔案");
            return null;
        }
        Object doc;
        try {
            doc = new Yaml().load(text);
        } catch (RuntimeException e) {
            loadProblems.add(name + "：YAML 讀不進來（" + e.getMessage().split("\n")[0] + "）");
            return null;
        }
        if (!(doc instanceof Map)) {
            loadProblems.add(name + "：最上層必須是對應表");
            return null;
        }
        Map<?, ?> m = (Map<?, ?>) doc;
        // D-088：use case 規格的格式 v2（D-065）也要讀得進來；http.yaml、http-common.yaml 仍是 1。
        boolean specFile = "usecases".equals(kind) || "aggregate".equals(kind);
        Object version = m.get("spec_version");
        if (!(Integer.valueOf(1).equals(version) || specFile && Integer.valueOf(2).equals(version))) {
            loadProblems.add(name + "：spec_version 必須是 1" + (specFile ? " 或 2" : ""));
        }
        if (!kind.equals(m.get("kind"))) loadProblems.add(name + "：kind 必須是 " + kind + "，實際是 " + m.get("kind"));
        return m;
    }

    private String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private String readFirst(Path first, Path second) {
        return Files.isRegularFile(first) ? read(first) : read(second);
    }

    private static Map<?, ?> map(Object o) {
        return o instanceof Map ? (Map<?, ?>) o : Map.of();
    }

    private static List<?> list(Object o) {
        return o instanceof List ? (List<?>) o : List.of();
    }

    private static boolean isVariable(String segment) {
        return segment.startsWith("{") && segment.endsWith("}");
    }

    private static String[] split(String path) {
        String p = path.startsWith("/") ? path.substring(1) : path;
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p.isEmpty() ? new String[0] : p.split("/", -1);
    }
}
