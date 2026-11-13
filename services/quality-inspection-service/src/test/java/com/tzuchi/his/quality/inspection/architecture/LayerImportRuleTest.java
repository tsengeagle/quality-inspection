package com.tzuchi.his.quality.inspection.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ARCH-04 的閘門與違規樣本。
 *
 * <p>既有違規凍結在 {@code src/test/resources/layer-import-baseline.txt}。
 * 實際違規必須與基線完全相同：多一筆是新增違規，少一筆要把基線那一行刪掉。
 */
class LayerImportRuleTest {

    /** Gradle 的 test 工作目錄是模組根目錄。 */
    private static final Path MAIN_SOURCES = Paths.get("src/main/java");
    private static final Path TEST_SOURCES = Paths.get("src/test/java");
    private static final Path BASELINE = Paths.get("src/test/resources/layer-import-baseline.txt");

    /** 掃描範圍的下限。掃到的檔案太少代表路徑設錯，不能算通過。 */
    private static final int MINIMUM_SCANNED_FILES = 200;

    @Test
    void arch04_mainSourcesMatchFrozenBaseline() throws IOException {
        LayerImportRule.Result result = LayerImportRule.scan(MAIN_SOURCES, ArchitectureRules.ROOT);

        assertThat(result.scannedFiles).as("ARCH-04 掃描的檔案數").isGreaterThanOrEqualTo(MINIMUM_SCANNED_FILES);
        assertThat(result.violations)
                .as("ARCH-04 分層依賴方向（import）：實際違規必須與 %s 完全相同", BASELINE)
                .containsExactlyElementsOf(readBaseline());
    }

    @Test
    void arch04_detectsEachForbiddenImportInSamples() {
        String sampleRoot = ArchitectureRules.ROOT + ".architecture.samples.layering";
        LayerImportRule.Result result = LayerImportRule.scan(TEST_SOURCES, sampleRoot);

        assertThat(result.scannedFiles).as("樣本檔案數").isGreaterThan(0);
        assertReported(result.violations, "domain/DomainUsesApplication.java");
        assertReported(result.violations, "application/ApplicationUsesInterfaces.java");
        assertReported(result.violations, "application/ApplicationUsesInfrastructure.java");
        assertReported(result.violations, "reporting/ReportingUsesInfrastructure.java");
        assertReported(result.violations, "interfaces/InterfacesUsesInfrastructure.java");
        assertReported(result.violations, "infrastructure/InfrastructureUsesInterfaces.java");
        assertReported(result.violations, "infrastructure/InfrastructureUsesReporting.java");
        // 只引用編譯期常數：ARCH-01 看不到，這條規則要看得到
        assertReported(result.violations, "interfaces/InterfacesUsesInfrastructureConstant.java");
        assertThat(result.violations).as("合規對照不應被報出")
                .noneMatch(v -> v.startsWith("interfaces/AllowedInterfacesUsesApplication.java"));
    }

    private static void assertReported(List<String> violations, String sampleFile) {
        assertThat(violations).as("規則應報出違規樣本 %s", sampleFile)
                .anyMatch(v -> v.startsWith(sampleFile + " imports "));
    }

    private static List<String> readBaseline() throws IOException {
        return Files.readAllLines(BASELINE, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .sorted()
                .collect(Collectors.toList());
    }
}
