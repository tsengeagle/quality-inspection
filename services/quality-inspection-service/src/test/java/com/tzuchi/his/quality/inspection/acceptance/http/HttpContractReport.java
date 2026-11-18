package com.tzuchi.his.quality.inspection.acceptance.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 測試 JVM 結束時，以契約比對觀察到的回應，寫出報告。
 *
 * <p>試做階段只評估 {@code spec-draft/} 的契約，<b>只列出、不擋</b>（與 spec-check 對 spec-draft 的處理相同）。
 * 升格到 {@code specs/} 時要接上閘門（例如讓 Gradle 讀報告的結果），屆時另以修法處理。</p>
 *
 * <p>系統屬性：{@code acceptance.specs.dir}（預設 ../../specs）、{@code acceptance.specdraft.dir}（預設 ../../spec-draft）、
 * {@code http.contract.report}（預設 build/reports/http-contract/report.txt）。</p>
 */
final class HttpContractReport {

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
        write(draft, specs, out, HttpObservationRecorder.snapshot());
    }

    /** 契約壞掉、比對中途出錯時，仍寫出本次的報告（結果為不符合），不會留下上一次的報告。 */
    static void write(Path draft, Path specs, Path out, List<HttpObservation> observations) throws IOException {

        StringBuilder text = new StringBuilder("HTTP 契約報告（B-1：觀察）\n");
        text.append("觀察到的 HTTP 交換：").append(observations.size()).append('\n');
        text.append("注意：只列出、不擋。只有完整執行測試時，涵蓋的結果才有意義。\n");
        Path common = draft.resolve("http-common.yaml");
        if (!Files.isRegularFile(common)) {
            text.append("\n").append(common).append(" 不存在：沒有試做中的契約\n");
        } else {
            HttpContract contract = HttpContract.load(common, draft, specs);
            HttpContractEvaluator evaluator = new HttpContractEvaluator(contract);
            List<String> problems;
            try {
                observations.forEach(evaluator::observe);
                problems = evaluator.problems();
            } catch (RuntimeException e) {
                problems = new java.util.ArrayList<>(contract.loadProblems());
                problems.add("比對中斷，本次結果不完整：" + e);
            }
            text.append("\n== ").append(draft).append("（試做，只列出）\n");
            text.append("契約端點 ").append(contract.endpoints().size())
                    .append("，契約範圍內的觀察 ").append(evaluator.observed()).append('\n');
            for (Map.Entry<String, int[]> e : evaluator.counts().entrySet()) {
                text.append(String.format("  %-50s 成功 %4d  失敗 %4d%n", e.getKey(), e.getValue()[0], e.getValue()[1]));
            }
            text.append("問題 ").append(problems.size()).append('\n');
            problems.forEach(p -> text.append("  - ").append(p).append('\n'));
            text.append("結果：").append(problems.isEmpty() ? "符合契約" : "不符合契約（試做階段只列出）").append('\n');
        }
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, text.toString(), StandardCharsets.UTF_8);
    }
}
