package com.tzuchi.his.quality.inspection.acceptance.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
 *   <li>涵蓋：契約裡每個端點至少觀察到一次成功回應；data 形狀裡的每個陣列至少觀察到一次非空，元素的形狀才算比對過。</li>
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
    private final Map<String, Set<String>> arraysSeen = new HashMap<>();
    private int observed;

    public HttpContractEvaluator(HttpContract contract) {
        this.contract = contract;
        for (HttpContract.Endpoint e : contract.endpoints()) {
            counts.put(e.key(), new int[2]);
            arraysSeen.put(e.key(), new HashSet<>());
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
            JsonShape.check(contract.successEnvelope(), body, "$", e.dataShape(), enums, out, arraysSeen.get(e.key()));
        } else {
            JsonShape.check(contract.errorEnvelope(), body, "$", null, enums, out, new HashSet<>());
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

    /** 載入問題、觀察到的問題、涵蓋問題。空的才算通過。 */
    public List<String> problems() {
        List<String> all = new ArrayList<>(contract.loadProblems());
        problems.forEach((p, n) -> all.add(n > 1 ? p + "（" + n + " 次）" : p));
        for (HttpContract.Endpoint e : contract.endpoints()) {
            if (counts.get(e.key())[0] == 0) {
                all.add(e.key() + "：沒有觀察到任何成功回應");
                continue;
            }
            for (String array : e.arrays()) {
                if (!arraysSeen.get(e.key()).contains(array)) {
                    all.add(e.key() + "：陣列 " + array + " 從未觀察到非空，元素的形狀沒有比對過");
                }
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
