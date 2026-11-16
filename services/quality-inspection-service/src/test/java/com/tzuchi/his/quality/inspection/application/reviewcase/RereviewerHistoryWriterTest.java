package com.tzuchi.his.quality.inspection.application.reviewcase;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-055 第 4 點：所有會變更複審醫師的入口都必須寫入複審指派歷史。
 * 變更複審醫師只能透過 ReviewCase 的兩個 domain 方法；這裡確認正式程式只有兩個 use case 呼叫它們，
 * 且這兩個 use case 都寫入歷史（行為由各自的 use case 測試驗證）。新增呼叫點時這個測試會失敗，
 * 提醒必須一併寫入歷史並更新這份清單。
 */
class RereviewerHistoryWriterTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Pattern CALL = Pattern.compile("\\.(re)?assignRereviewer\\s*\\(");
    private static final Set<String> ALLOWED = Set.of(
            "com/tzuchi/his/quality/inspection/application/reviewcase/AssignRereviewerUseCase.java",
            "com/tzuchi/his/quality/inspection/application/reviewcase/ReassignRereviewerUseCase.java");

    @Test
    void onlyTheTwoUseCasesChangeTheRereviewer_andBothAppendHistory() throws IOException {
        List<String> callers;
        try (Stream<Path> files = Files.walk(MAIN)) {
            callers = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> CALL.matcher(read(p)).find())
                    .map(p -> MAIN.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertThat(callers).as("呼叫 assignRereviewer／reassignRereviewer 的正式程式").containsExactlyInAnyOrderElementsOf(ALLOWED);
        for (String allowed : ALLOWED) {
            assertThat(read(MAIN.resolve(allowed))).as(allowed + " 必須寫入複審指派歷史").contains("historyPort.append(");
        }
    }

    private static String read(Path p) {
        try {
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
