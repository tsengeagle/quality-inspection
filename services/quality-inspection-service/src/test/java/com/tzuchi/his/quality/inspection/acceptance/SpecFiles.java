package com.tzuchi.his.quality.inspection.acceptance;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 讀規格目錄。預設是 repo 根目錄的 specs/（相對於後端模組為 ../../specs），可用系統屬性 acceptance.specs.dir 指定。
 */
final class SpecFiles {

    private final Path root;

    SpecFiles(Path root) {
        this.root = root;
    }

    static SpecFiles fromSystemProperty() {
        return new SpecFiles(Paths.get(System.getProperty("acceptance.specs.dir", "../../specs")).toAbsolutePath().normalize());
    }

    Path root() {
        return root;
    }

    /** 規格目錄（specs/ 底下的每個子目錄），依名稱排序。 */
    List<Path> directories() {
        if (!Files.isDirectory(root)) {
            return Collections.emptyList();
        }
        try (Stream<Path> dirs = Files.list(root)) {
            return dirs.filter(Files::isDirectory).sorted().collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 規格目錄名稱 → examples.yaml。沒有 examples.yaml 的目錄不在裡面。 */
    Map<String, Map<String, Object>> examples() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Path directory : directories()) {
            Path file = directory.resolve("examples.yaml");
            if (Files.isRegularFile(file)) {
                out.put(directory.getFileName().toString(), load(file));
            }
        }
        return out;
    }

    /** 所有 usecases.yaml 定義的 use case 名稱。 */
    Set<String> useCaseNames() {
        return names("usecases.yaml", "usecases");
    }

    /** 所有 model.yaml 宣告的埠（D-065 第 11 點）：只有這些可以被替換。 */
    Set<String> declaredPorts() {
        return names("model.yaml", "ports");
    }

    /** spec-profile.yaml；不存在就是空的。 */
    Map<String, Object> profile() {
        Path file = root.resolve("spec-profile.yaml");
        return Files.isRegularFile(file) ? load(file) : Collections.emptyMap();
    }

    /** http-common.yaml（D-061）；不存在就是空的。http 步驟用它推得狀態碼（D-074 第 4 點）。 */
    Map<String, Object> httpCommon() {
        Path file = root.resolve("http-common.yaml");
        return Files.isRegularFile(file) ? load(file) : Collections.emptyMap();
    }

    /** 一個規格目錄的錯誤代碼 → 分類（usecases.yaml 的 error_codes）。沒有 usecases.yaml 就是空的。 */
    Map<String, String> errorCategories(String directory) {
        Map<String, String> out = new LinkedHashMap<>();
        Path file = root.resolve(directory).resolve("usecases.yaml");
        if (!Files.isRegularFile(file)) {
            return out;
        }
        Object items = load(file).get("error_codes");
        for (Object item : items instanceof List ? (List<?>) items : new ArrayList<>()) {
            if (item instanceof Map && ((Map<?, ?>) item).get("code") instanceof String
                    && ((Map<?, ?>) item).get("category") instanceof String) {
                out.put((String) ((Map<?, ?>) item).get("code"), (String) ((Map<?, ?>) item).get("category"));
            }
        }
        return out;
    }

    private Set<String> names(String fileName, String listField) {
        Set<String> out = new HashSet<>();
        for (Path directory : directories()) {
            Path file = directory.resolve(fileName);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            Object items = load(file).get(listField);
            for (Object item : items instanceof List ? (List<?>) items : new ArrayList<>()) {
                if (item instanceof Map && ((Map<?, ?>) item).get("name") instanceof String) {
                    out.add((String) ((Map<?, ?>) item).get("name"));
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> load(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object doc = new Yaml().load(reader);
            if (!(doc instanceof Map)) {
                throw new AssertionError(file + " 的最上層必須是對應表");
            }
            return (Map<String, Object>) doc;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
