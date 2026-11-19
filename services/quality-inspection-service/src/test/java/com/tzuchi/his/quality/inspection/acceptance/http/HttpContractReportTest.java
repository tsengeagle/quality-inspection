package com.tzuchi.his.quality.inspection.acceptance.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 報告：{@code specs/} 的契約是強制的，{@code spec-draft/} 只列出（D-061）。
 * Gradle 的閘門只讀報告開頭的 {@code run-id:} 與 {@code enforced-problems:} 兩行，以及 {@code [擋]} 開頭的問題。
 */
class HttpContractReportTest {

    private static final String COMMON = String.join("\n",
            "spec_version: 1", "kind: http-common",
            "envelope:",
            "  success: { success: 'true', data: $data }",
            "  error: { success: 'false', error: { code: text } }",
            "status_by_category: { not_found: 404 }");
    private static final String USECASES = String.join("\n",
            "spec_version: 1", "kind: usecases", "usecases:", "  - name: GetThing", "  - name: GetOther");
    private static final String GOOD = "{\"success\":true,\"data\":{\"id\":\"T1\"}}";
    private static final String BARE_ARRAY = "{\"success\":true,\"data\":[]}";

    private static void contract(Path root, String aggregate, String method, String path, String usecase) throws IOException {
        Files.createDirectories(root.resolve(aggregate));
        Files.writeString(root.resolve("http-common.yaml"), COMMON, StandardCharsets.UTF_8);
        Files.writeString(root.resolve(aggregate).resolve("http.yaml"), String.join("\n",
                "spec_version: 1", "kind: http", "endpoints:",
                "  - { usecase: " + usecase + ", method: " + method + ", path: " + path
                        + ", success: { status: 200, data: { id: id } } }"), StandardCharsets.UTF_8);
        Files.writeString(root.resolve(aggregate).resolve("usecases.yaml"), USECASES, StandardCharsets.UTF_8);
    }

    private static String report(Path dir, boolean fullRun, List<HttpObservation> observations) throws IOException {
        Path out = dir.resolve("report.txt");
        HttpContractReport.write(dir.resolve("specs"), dir.resolve("draft"), out, "RUN-1", fullRun, observations);
        return Files.readString(out, StandardCharsets.UTF_8);
    }

    @Test
    void specsAreEnforcedAndDraftIsReportOnly(@TempDir Path dir) throws Exception {
        contract(dir.resolve("specs"), "thing", "GET", "/things", "GetThing");
        contract(dir.resolve("draft"), "other", "GET", "/others", "GetOther");
        String text = report(dir, true, List.of(
                new HttpObservation("GET", "/things", 200, BARE_ARRAY),
                new HttpObservation("GET", "/others", 200, BARE_ARRAY)));

        assertTrue(text.startsWith("run-id: RUN-1\nenforced-problems: 1\n"), text);
        assertTrue(text.contains("[擋] GET /things（觀察到 200 GET /things） $.data：預期物件，實際是陣列"), text);
        assertTrue(text.contains("GET /others"), text);
        assertFalse(text.contains("[擋] GET /others"), text);
    }

    @Test
    void conformingRunHasNoEnforcedProblems(@TempDir Path dir) throws Exception {
        contract(dir.resolve("specs"), "thing", "GET", "/things", "GetThing");
        String text = report(dir, true, List.of(new HttpObservation("GET", "/things", 200, GOOD)));
        assertTrue(text.startsWith("run-id: RUN-1\nenforced-problems: 0\n"), text);
        assertFalse(text.contains("[擋]"), text);
    }

    @Test
    void coverageIsEnforcedOnlyOnAFullRun(@TempDir Path dir) throws Exception {
        contract(dir.resolve("specs"), "thing", "GET", "/things", "GetThing");
        String filtered = report(dir, false, List.of());
        assertTrue(filtered.startsWith("run-id: RUN-1\nenforced-problems: 0\n"), filtered);
        assertTrue(filtered.contains("GET /things：沒有觀察到任何成功回應"), filtered);

        String full = report(dir, true, List.of());
        assertTrue(full.startsWith("run-id: RUN-1\nenforced-problems: 1\n"), full);
        assertTrue(full.contains("[擋] GET /things：沒有觀察到任何成功回應"), full);
    }

    @Test
    void missingSpecsContractIsAnEnforcedProblem(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("specs"));
        String text = report(dir, true, List.of());
        assertTrue(text.startsWith("run-id: RUN-1\nenforced-problems: 1\n"), text);
        assertTrue(text.contains("[擋]"), text);
        assertTrue(text.contains("http-common.yaml"), text);
    }

    /** 契約壞掉時報告仍要寫出本次的結果，不能中止而留下上一次的報告（Copilot review，#93）。 */
    @Test
    void brokenContractStillWritesThisRunsReport(@TempDir Path dir) throws Exception {
        Path specs = Files.createDirectories(dir.resolve("specs"));
        Files.writeString(specs.resolve("http-common.yaml"), String.join("\n",
                "spec_version: 1", "kind: http-common",
                "envelope:", "  success: { success: true, data: $data }", "  error: { error: [] }",
                "status_by_category: { denied: 403 }"), StandardCharsets.UTF_8);
        Files.createDirectories(specs.resolve("thing"));
        Files.writeString(specs.resolve("thing/http.yaml"), String.join("\n",
                "spec_version: 1", "kind: http", "endpoints:",
                "  - { usecase: GetThing, method: GET, path: /things, success: { status: 200, data: { a: [] } } }"),
                StandardCharsets.UTF_8);
        Files.writeString(specs.resolve("thing/usecases.yaml"), USECASES, StandardCharsets.UTF_8);
        Path out = dir.resolve("report.txt");
        Files.writeString(out, "上一次的舊報告：符合契約", StandardCharsets.UTF_8);

        HttpContractReport.write(specs, dir.resolve("draft"), out, "RUN-1", true, List.of(
                new HttpObservation("GET", "/things", 200, "{\"success\":true,\"data\":{\"a\":[1]}}"),
                new HttpObservation("GET", "/things", 500, "{\"error\":{\"code\":\"X\"}}")));

        String text = Files.readString(out, StandardCharsets.UTF_8);
        assertFalse(text.contains("上一次的舊報告"), text);
        assertTrue(text.contains("陣列的形狀必須剛好一個元素"), text);
        assertFalse(text.startsWith("run-id: RUN-1\nenforced-problems: 0\n"), text);
    }

    /** 升格後草稿區沒有自己的共用層，只放 <aggregate>/http.yaml 時，沿用 specs/ 的共用層照樣比對（Copilot review，#98）。 */
    @Test
    void draftEndpointsWithoutDraftCommonUseSpecsCommonAndAreReportOnly(@TempDir Path dir) throws Exception {
        contract(dir.resolve("specs"), "thing", "GET", "/things", "GetThing");
        Path draft = dir.resolve("draft");
        contract(draft, "other", "GET", "/others", "GetOther");
        Files.delete(draft.resolve("http-common.yaml"));
        String text = report(dir, true, List.of(
                new HttpObservation("GET", "/things", 200, GOOD),
                new HttpObservation("GET", "/others", 200, BARE_ARRAY)));

        assertTrue(text.startsWith("run-id: RUN-1\nenforced-problems: 0\n"), text);
        assertTrue(text.contains("（草稿，只列出）"), text);
        assertTrue(text.contains("- GET /others（觀察到 200 GET /others） $.data：預期物件，實際是陣列"), text);
    }

    @Test
    void emptyDraftDirectoryIsNotReported(@TempDir Path dir) throws Exception {
        contract(dir.resolve("specs"), "thing", "GET", "/things", "GetThing");
        Files.createDirectories(dir.resolve("draft"));
        String text = report(dir, true, List.of(new HttpObservation("GET", "/things", 200, GOOD)));
        assertFalse(text.contains("草稿"), text);
    }
}
