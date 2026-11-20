package com.tzuchi.his.quality.inspection.acceptance;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 把 specs/ 底下每個規格目錄的 examples.yaml 逐一當成驗收測試執行（第 4 期）。
 *
 * <p>一個範例一個測試，名稱是 {@code <目錄>/<範例名稱>}；未通過的範例由 known-test-failures.txt 的閘門核對。
 * 每個範例在一個交易裡執行，結束後還原，範例之間互不影響。</p>
 *
 * <p>規格目錄預設是 repo 根目錄的 specs/（相對於後端模組為 ../../specs），可用系統屬性 acceptance.specs.dir 指定。
 * 找不到任何範例時測試失敗：沒有評估不能算通過。</p>
 *
 * <p>spec_version 2 的範例（D-065）另外需要時鐘、事件與埠，由 {@link AcceptanceWorldConfiguration} 提供。</p>
 */
@SpringBootTest
@Import(AcceptanceWorldConfiguration.class)
class AcceptanceExamplesTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SpringExampleWorld world;

    @TestFactory
    Stream<DynamicTest> examples() {
        SpecFiles specs = SpecFiles.fromSystemProperty();
        Map<String, Map<String, Object>> documents = specs.examples();
        if (documents.isEmpty()) {
            throw new AssertionError("在 " + specs.root() + " 找不到任何 examples.yaml；驗收範例沒有被評估");
        }

        Set<String> specified = specs.useCaseNames();
        Map<String, Object> profile = specs.profile();
        UseCaseInvoker invoker = new SpringUseCaseInvoker(context, world);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<DynamicTest> tests = new ArrayList<>();
        for (String directory : documents.keySet()) {
            ExampleRunner runner = new ExampleRunner(directory, documents, profile, specified, invoker, world);
            for (String name : runner.exampleNames()) {
                tests.add(DynamicTest.dynamicTest(directory + "/" + name, () -> transaction.executeWithoutResult(status -> {
                    status.setRollbackOnly();
                    runner.run(name);
                })));
            }
        }
        if (tests.isEmpty()) {
            throw new AssertionError("在 " + specs.root() + " 的 examples.yaml 裡找不到任何範例；驗收範例沒有被評估");
        }
        return tests.stream();
    }
}
