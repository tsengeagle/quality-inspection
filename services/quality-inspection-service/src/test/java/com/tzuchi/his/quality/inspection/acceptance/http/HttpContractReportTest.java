package com.tzuchi.his.quality.inspection.acceptance.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 契約壞掉時報告仍要寫出本次的結果，不能中止而留下上一次的報告（Copilot review，#93）。 */
class HttpContractReportTest {

    @Test
    void brokenContractStillWritesThisRunsReport(@TempDir Path dir) throws Exception {
        Path draft = Files.createDirectories(dir.resolve("draft"));
        Path specs = Files.createDirectories(dir.resolve("specs"));
        Files.writeString(draft.resolve("http-common.yaml"), String.join("\n",
                "spec_version: 1", "kind: http-common",
                "envelope:", "  success: { success: true, data: $data }", "  error: { error: [] }",
                "status_by_category: { denied: 403 }"), StandardCharsets.UTF_8);
        Files.createDirectories(draft.resolve("thing"));
        Files.writeString(draft.resolve("thing/http.yaml"), String.join("\n",
                "spec_version: 1", "kind: http", "endpoints:",
                "  - { usecase: GetThing, method: GET, path: /things, success: { status: 200, data: { a: [] } } }"),
                StandardCharsets.UTF_8);
        Files.writeString(draft.resolve("thing/usecases.yaml"), String.join("\n",
                "spec_version: 1", "kind: usecases", "usecases:", "  - name: GetThing"), StandardCharsets.UTF_8);
        Path out = dir.resolve("report.txt");
        Files.writeString(out, "上一次的舊報告：符合契約", StandardCharsets.UTF_8);

        HttpContractReport.write(draft, specs, out, List.of(
                new HttpObservation("GET", "/things", 200, "{\"success\":true,\"data\":{\"a\":[1]}}"),
                new HttpObservation("GET", "/things", 500, "{\"error\":{\"code\":\"X\"}}")));

        String text = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(!text.contains("上一次的舊報告"), text);
        assertTrue(text.contains("陣列的形狀必須剛好一個元素"), text);
        assertTrue(text.contains("不符合契約"), text);
    }
}
