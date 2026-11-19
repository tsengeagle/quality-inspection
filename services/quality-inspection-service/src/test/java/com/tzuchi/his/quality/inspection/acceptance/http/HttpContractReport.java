package com.tzuchi.his.quality.inspection.acceptance.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 測試 JVM 結束時，以契約比對觀察到的回應，寫出報告（D-061）。
 *
 * <ul>
 *   <li>{@code specs/} 的契約是<b>強制的</b>：問題數寫在報告第二行 {@code enforced-problems: N}，每個問題以 {@code [擋]} 開頭。
 *       {@code build.gradle} 的 test 閘門讀這兩樣東西，N 大於 0 就未通過。</li>
 *   <li>{@code spec-draft/} 的契約（有的話）<b>只列出、不擋</b>，與 spec-check 對 spec-draft 的處理相同。</li>
 *   <li>報告第一行是 {@code run-id:}，值是 Gradle 這次執行給的識別碼。閘門靠它確認報告是本次產生的，
 *       觀察器沒有運作、只留下舊報告，都會被發現。</li>
 *   <li>涵蓋問題（端點沒有成功觀察、陣列從未非空、可為 null 的欄位只觀察到一種值）只在完整執行時才算進強制的問題；
 *       有篩選條件時照樣列出，但不擋。</li>
 * </ul>
 *
 * <p>系統屬性：{@code acceptance.specs.dir}（預設 ../../specs）、{@code acceptance.specdraft.dir}（預設 ../../spec-draft）、
 * {@code http.contract.report}（預設 build/reports/http-contract/report.txt）、{@code http.contract.runId}、
 * {@code http.contract.fullRun}（預設 false）。</p>
 */
final class HttpContractReport {

    static final String BLOCK = "[擋] ";

    private HttpContractReport() {}

    static void writeQuietly() {
        try {
            write();
        } catch (Exception | Error e) {
            System.err.println("HTTP 契約報告寫入失敗：" + e);
        }
    }

    static void write() throws IOException {
        Path specs = Paths.get(System.getProperty("acceptance.specs.dir", "../../specs")).toAbsolutePath().normalize();
        Path draft = Paths.get(System.getProperty("acceptance.specdraft.dir", "../../spec-draft")).toAbsolutePath().normalize();
        Path out = Paths.get(System.getProperty("http.contract.report", "build/reports/http-contract/report.txt"));
        write(specs, draft, out, System.getProperty("http.contract.runId", "none"),
                Boolean.parseBoolean(System.getProperty("http.contract.fullRun", "false")), HttpObservationRecorder.snapshot());
    }

    /** 契約壞掉、比對中途出錯時，仍寫出本次的報告（判為有問題），不會留下上一次的報告。 */
    static void write(Path specs, Path draft, Path out, String runId, boolean fullRun,
                      List<HttpObservation> observations) throws IOException {
        StringBuilder body = new StringBuilder();
        body.append("HTTP 契約報告（B-1：觀察，D-061）\n");
        body.append("觀察到的 HTTP 交換：").append(observations.size()).append('\n');
        body.append(fullRun ? "完整執行：涵蓋問題算進強制的問題\n" : "有篩選條件：涵蓋問題只列出、不擋\n");

        List<String> enforced = new ArrayList<>();
        Path common = specs.resolve("http-common.yaml");
        if (!Files.isRegularFile(common)) {
            String missing = common + " 不存在：D-061 生效後，specs/ 必須有共用層契約";
            enforced.add(missing);
            body.append("\n== ").append(specs).append("（強制）\n  ").append(BLOCK).append(missing).append('\n');
        } else {
            Section s = evaluate(common, specs, specs, observations);
            enforced.addAll(s.observation);
            if (fullRun) enforced.addAll(s.coverage);
            body.append("\n== ").append(specs).append("（強制）\n").append(s.summary);
            for (String p : s.observation) body.append("  ").append(BLOCK).append(p).append('\n');
            for (String p : s.coverage) body.append("  ").append(fullRun ? BLOCK : "- ").append(p).append('\n');
        }

        Path draftCommon = draft.resolve("http-common.yaml");
        if (Files.isRegularFile(draftCommon)) {
            Section s = evaluate(draftCommon, draft, specs, observations);
            body.append("\n== ").append(draft).append("（草稿，只列出）\n").append(s.summary);
            for (String p : s.observation) body.append("  - ").append(p).append('\n');
            for (String p : s.coverage) body.append("  - ").append(p).append('\n');
        }

        body.append("\n結果：").append(enforced.isEmpty() ? "符合契約" : "不符合契約（" + enforced.size() + "）").append('\n');
        String text = "run-id: " + runId + "\nenforced-problems: " + enforced.size() + "\n" + body;
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, text, StandardCharsets.UTF_8);
    }

    private static final class Section {
        final List<String> observation = new ArrayList<>();
        final List<String> coverage = new ArrayList<>();
        final StringBuilder summary = new StringBuilder();
    }

    private static Section evaluate(Path common, Path root, Path specs, List<HttpObservation> observations) {
        Section s = new Section();
        HttpContract contract = HttpContract.load(common, root, specs);
        HttpContractEvaluator evaluator = new HttpContractEvaluator(contract);
        try {
            observations.forEach(evaluator::observe);
            s.observation.addAll(evaluator.observationProblems());
            s.coverage.addAll(evaluator.coverageProblems());
        } catch (RuntimeException e) {
            s.observation.addAll(contract.loadProblems());
            s.observation.add("比對中斷，本次結果不完整：" + e);
        }
        s.summary.append("契約端點 ").append(contract.endpoints().size())
                .append("，契約範圍內的觀察 ").append(evaluator.observed()).append('\n');
        for (Map.Entry<String, int[]> e : evaluator.counts().entrySet()) {
            s.summary.append(String.format("  %-50s 成功 %4d  失敗 %4d%n", e.getKey(), e.getValue()[0], e.getValue()[1]));
        }
        s.summary.append("問題 ").append(s.observation.size() + s.coverage.size()).append('\n');
        return s;
    }
}
