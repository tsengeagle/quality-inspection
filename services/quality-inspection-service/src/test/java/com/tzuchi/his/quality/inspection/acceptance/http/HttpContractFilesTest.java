package com.tzuchi.his.quality.inspection.acceptance.http;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * repo 裡的契約檔本身要讀得進來：形狀、use case 歸屬、列舉、錯誤分類都要對得上（D-061 第 2 點）。
 * {@code spec_lint} 不檢查 {@code http.yaml} 與 {@code http-common.yaml}，由本測試檢查。
 */
class HttpContractFilesTest {

    private static final Path SPECS = Paths.get(System.getProperty("acceptance.specs.dir", "../../specs")).toAbsolutePath().normalize();

    @Test
    void specsContractLoadsWithoutProblems() {
        Path common = SPECS.resolve("http-common.yaml");
        assertTrue(Files.isRegularFile(common), common + " 不存在");
        HttpContract contract = HttpContract.load(common, SPECS, SPECS);
        assertEquals(List.of(), contract.loadProblems());
        assertEquals(52, contract.endpoints().size(),
                "score-sheet 7 個、review-project 9 個、review-case 29 個、sampling-batch 7 個端點（D-088），一支 use case 一個");
    }

}
