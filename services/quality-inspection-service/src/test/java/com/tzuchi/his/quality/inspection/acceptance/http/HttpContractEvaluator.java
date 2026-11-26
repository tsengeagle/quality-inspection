package com.tzuchi.his.quality.inspection.acceptance.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * B-1（觀察）：以契約比對測試期間觀察到的每一個 HTTP 回應，再檢查涵蓋。
 *
 * <ul>
 *   <li>每個回應：成功的比對外殼、狀態碼與 data 形狀；失敗的比對錯誤外殼，以及錯誤代碼的分類對應的狀態碼。</li>
 *   <li>涵蓋：契約裡每個端點至少觀察到一次成功回應；data 形狀裡的每個陣列至少觀察到一次非空，元素的形狀才算比對過；
 *       可為 null 的欄位，null 與非 null 都要觀察到（W-7，D-061）。</li>
 * </ul>
 *
 * <p>契約以外的端點不比對（那些範圍還沒有 HTTP 契約）。</p>
 */
public final class HttpContractEvaluator {

    /** 本文必須剛好是一個 JSON 文件：Jackson 2.x 預設不檢查第一個根值之後還有沒有東西。 */
    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private final HttpContract contract;
    private final Map<String, Integer> problems = new LinkedHashMap<>();   // 問題 -> 次數
    private final Map<String, int[]> counts = new LinkedHashMap<>();       // 端點 -> {成功, 失敗}
    private final Map<String, JsonShape.Seen> seen = new HashMap<>();
    private int observed;

    public HttpContractEvaluator(HttpContract contract) {
        this.contract = contract;
        for (HttpContract.Endpoint e : contract.endpoints()) {
            counts.put(e.key(), new int[2]);
            for (HttpContract.Variant v : e.variants()) {
                seen.put(seenKey(e, v), new JsonShape.Seen());
            }
        }
    }

    public void observe(HttpObservation o) {
        Optional<HttpContract.Endpoint> match = contract.match(o.method(), o.uri());
        if (match.isEmpty()) return;
        HttpContract.Endpoint e = match.get();
        observed++;
        String where = e.key() + "（觀察到 " + o + "）";
        boolean success = o.status() >= 200 && o.status() < 300;
        counts.get(e.key())[success ? 0 : 1]++;

        JsonNode body;
        try {
            body = JSON.readTree(o.body() == null ? "" : o.body());
        } catch (Exception ex) {
            body = null;
        }
        if (body == null || body.isMissingNode()) {
            add(where + "：回應不是單一 JSON 文件");
            return;
        }
        List<String> out = new ArrayList<>();
        Map<String, Set<String>> enums = contract.enumsOf(e.aggregate());
        if (success) {
            if (o.status() != e.successStatus()) {
                out.add("成功狀態碼應為 " + e.successStatus() + "，實際 " + o.status());
            }
            checkSuccess(e, body, enums, out);
        } else {
            JsonShape.check(contract.errorEnvelope(), body, "$", null, enums, out, new JsonShape.Seen());
            JsonNode code = body.path("error").path("code");
            if (code.isTextual()) {
                String[] reason = new String[1];
                Integer expected = contract.expectedStatus(e, code.textValue(), reason);
                if (expected == null) {
                    out.add("錯誤代碼 " + code.textValue() + " 沒有定義（不在 " + e.aggregate()
                            + " 的 usecases.yaml，也不在 http-common.yaml 的 transport_codes）");
                } else if (expected != o.status()) {
                    out.add(reason[0] + "，實際 " + o.status());
                }
            }
        }
        for (String p : out) add(where + " " + p);
    }

    /**
     * 成功回應：只有一種形狀時照常比對。有幾種形狀時（D-088），回應必須剛好符合其中一種，
     * 觀察到的東西只算進符合的那一種；一種都不符合時，列出最接近的那一種的問題。
     */
    private void checkSuccess(HttpContract.Endpoint e, JsonNode body, Map<String, Set<String>> enums, List<String> out) {
        List<HttpContract.Variant> variants = e.variants();
        if (variants.size() == 1) {
            JsonShape.check(contract.successEnvelope(), body, "$", variants.get(0).shape(), enums, out, seen.get(seenKey(e, variants.get(0))));
            return;
        }
        List<HttpContract.Variant> matched = new ArrayList<>();
        List<JsonShape.Seen> matchedSeen = new ArrayList<>();
        List<String> closest = null;
        String closestName = null;
        for (HttpContract.Variant v : variants) {
            List<String> vOut = new ArrayList<>();
            JsonShape.Seen vSeen = new JsonShape.Seen();
            JsonShape.check(contract.successEnvelope(), body, "$", v.shape(), enums, vOut, vSeen);
            if (vOut.isEmpty()) {
                matched.add(v);
                matchedSeen.add(vSeen);
            } else if (closest == null || vOut.size() < closest.size()) {
                closest = vOut;
                closestName = v.name();
            }
        }
        if (matched.size() == 1) {
            JsonShape.Seen target = seen.get(seenKey(e, matched.get(0)));
            target.observed++;
            target.arrays.addAll(matchedSeen.get(0).arrays);
            target.nulls.addAll(matchedSeen.get(0).nulls);
            target.values.addAll(matchedSeen.get(0).values);
            return;
        }
        if (matched.size() > 1) {
            out.add("同時符合不只一種形狀（" + matched.stream().map(HttpContract.Variant::name).collect(java.util.stream.Collectors.joining("、"))
                    + "），無法判斷是哪一種");
            return;
        }
        out.add("不符合任何一種形狀；最接近的是 " + closestName + "：");
        out.addAll(closest);
    }

    private static String seenKey(HttpContract.Endpoint e, HttpContract.Variant v) {
        return v.name() == null ? e.key() : e.key() + "（" + v.name() + "）";
    }

    /** 載入問題、觀察到的問題、涵蓋問題。空的才算通過。 */
    public List<String> problems() {
        List<String> all = observationProblems();
        all.addAll(coverageProblems());
        return all;
    }

    /** 載入問題與每一筆回應的問題。與執行了哪些測試無關，有篩選條件時也要判。 */
    public List<String> observationProblems() {
        List<String> all = new ArrayList<>(contract.loadProblems());
        problems.forEach((p, n) -> all.add(n > 1 ? p + "（" + n + " 次）" : p));
        return all;
    }

    /** 涵蓋問題：沒有觀察到成功回應、陣列從未非空、可為 null 的欄位只觀察到一種值。只有完整執行時才有意義。 */
    public List<String> coverageProblems() {
        List<String> all = new ArrayList<>();
        for (HttpContract.Endpoint e : contract.endpoints()) {
            if (counts.get(e.key())[0] == 0) {
                all.add(e.key() + "：沒有觀察到任何成功回應");
                continue;
            }
            for (HttpContract.Variant v : e.variants()) {
                if (v.transition() != null) continue;  // D-094：過渡中的形狀不檢查涵蓋，見 transitions()
                String key = seenKey(e, v);
                JsonShape.Seen s = seen.get(key);
                if (v.name() != null && s.observed == 0) {
                    all.add(key + "：沒有觀察到這種形狀的成功回應");
                    continue;
                }
                for (String array : v.arrays()) {
                    if (!s.arrays.contains(array)) {
                        all.add(key + "：陣列 " + array + " 從未觀察到非空，元素的形狀沒有比對過");
                    }
                }
                for (String field : v.nullables()) {
                    if (!s.values.contains(field)) {
                        all.add(key + "：可為 null 的欄位 " + field + " 從未觀察到非 null 的值，值的形狀沒有比對過");
                    }
                    if (!s.nulls.contains(field)) {
                        all.add(key + "：可為 null 的欄位 " + field + " 從未觀察到 null");
                    }
                }
            }
        }
        return all;
    }

    /** D-094：過渡中的形狀（涵蓋沒有被評估），每項是「端點（形狀）：裁決編號，觀察到 n 次」。 */
    public List<String> transitions() {
        List<String> all = new ArrayList<>();
        for (HttpContract.Endpoint e : contract.endpoints()) {
            for (HttpContract.Variant v : e.variants()) {
                if (v.transition() == null) continue;
                String key = seenKey(e, v);
                all.add(key + "：" + v.transition() + " 過渡中，涵蓋沒有評估（觀察到 " + seen.get(key).observed + " 次）");
            }
        }
        return all;
    }

    public int observed() { return observed; }

    /** 每個端點的觀察次數：{成功, 失敗}。 */
    public Map<String, int[]> counts() { return counts; }

    private void add(String problem) {
        problems.merge(problem, 1, Integer::sum);
    }
}
