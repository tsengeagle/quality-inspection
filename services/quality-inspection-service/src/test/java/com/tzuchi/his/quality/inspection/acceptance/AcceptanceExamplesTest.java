package com.tzuchi.his.quality.inspection.acceptance;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 把 specs/ 底下每個 aggregate 的 examples.yaml 逐一當成驗收測試執行（第 4 期）。
 *
 * <p>一個範例一個測試，名稱是 {@code <aggregate>/<範例名稱>}；未通過的範例由 known-test-failures.txt 的閘門核對。
 * 每個範例在一個交易裡執行，結束後還原，範例之間互不影響。</p>
 *
 * <p>規格目錄預設是 repo 根目錄的 specs/（相對於後端模組為 ../../specs），可用系統屬性 acceptance.specs.dir 指定。
 * 找不到任何範例時測試失敗：沒有評估不能算通過。</p>
 */
@SpringBootTest
class AcceptanceExamplesTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @TestFactory
    Stream<DynamicTest> examples() throws IOException {
        Path specs = Paths.get(System.getProperty("acceptance.specs.dir", "../../specs")).toAbsolutePath().normalize();
        List<Path> aggregates = new ArrayList<>();
        if (Files.isDirectory(specs)) {
            try (Stream<Path> dirs = Files.list(specs)) {
                aggregates = dirs.filter(d -> Files.isRegularFile(d.resolve("examples.yaml")))
                        .sorted().collect(Collectors.toList());
            }
        }
        if (aggregates.isEmpty()) {
            throw new AssertionError("在 " + specs + " 找不到任何 examples.yaml；驗收範例沒有被評估");
        }

        Set<String> specified = new HashSet<>();
        for (Path aggregate : aggregates) {
            specified.addAll(useCaseNames(aggregate.resolve("usecases.yaml")));
        }

        UseCaseInvoker invoker = new SpringUseCaseInvoker(context);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<DynamicTest> tests = new ArrayList<>();
        for (Path aggregate : aggregates) {
            ExampleRunner runner = new ExampleRunner(load(aggregate.resolve("examples.yaml")), specified, invoker);
            String prefix = aggregate.getFileName().toString();
            for (String name : runner.exampleNames()) {
                tests.add(DynamicTest.dynamicTest(prefix + "/" + name, () -> transaction.executeWithoutResult(status -> {
                    status.setRollbackOnly();
                    runner.run(name);
                })));
            }
        }
        return tests.stream();
    }

    private static Set<String> useCaseNames(Path usecases) throws IOException {
        Set<String> names = new HashSet<>();
        if (!Files.isRegularFile(usecases)) {
            return names;
        }
        Object list = load(usecases).get("usecases");
        if (list instanceof List) {
            for (Object item : (List<?>) list) {
                if (item instanceof Map && ((Map<?, ?>) item).get("name") instanceof String) {
                    names.add((String) ((Map<?, ?>) item).get("name"));
                }
            }
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object doc = new Yaml().load(reader);
            if (!(doc instanceof Map)) {
                throw new AssertionError(file + " 的最上層必須是對應表");
            }
            return (Map<String, Object>) doc;
        }
    }
}
