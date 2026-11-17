package com.tzuchi.his.quality.inspection.domain.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResourceNotFoundException 的第一個參數是錯誤代碼（API 的 error.code），不是實體名稱。
 * 曾有 24 處傳入 "ReviewCase" 等實體名稱，讓 API 回出不在契約中的代碼；這個測試防止再發生。
 */
class ResourceNotFoundCodeTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Pattern CALL = Pattern.compile("new\\s+ResourceNotFoundException\\(\\s*\"([^\"]*)\"\\s*,");
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]*NOT_FOUND");

    @Test
    void everyErrorCodeLiteralIsAnUpperSnakeNotFoundCode() throws IOException {
        List<String> found = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        List<Path> sources;
        try (Stream<Path> files = Files.walk(MAIN)) {
            sources = files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList());
        }
        for (Path p : sources) {
            Matcher m = CALL.matcher(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
            while (m.find()) {
                found.add(m.group(1));
                if (!CODE.matcher(m.group(1)).matches()) bad.add(MAIN.relativize(p) + ": \"" + m.group(1) + "\"");
            }
        }
        assertThat(found).as("至少要掃到既有的呼叫，避免掃描失效").hasSizeGreaterThan(20);
        assertThat(bad).as("ResourceNotFoundException 的錯誤代碼必須是 *_NOT_FOUND").isEmpty();
    }
}
