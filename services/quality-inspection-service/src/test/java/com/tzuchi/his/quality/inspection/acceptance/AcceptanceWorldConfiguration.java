package com.tzuchi.his.quality.inspection.acceptance;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 驗收範例的 context 多出來的三樣東西（D-065）：可以固定的時鐘、事件的擷取、埠的替身。
 * 只有 {@link AcceptanceExamplesTest} 匯入；其他測試的 context 不受影響。
 */
@TestConfiguration
public class AcceptanceWorldConfiguration {

    /** 取代應用程式的系統時鐘。沒有固定的時候它就是系統時鐘。 */
    @Bean
    @Primary
    public AcceptanceClock acceptanceClock() {
        return new AcceptanceClock();
    }

    /** BeanPostProcessor 要在其他 bean 之前建立，所以是 static，自己讀規格宣告了哪些埠。 */
    @Bean
    public static AcceptancePorts acceptancePorts() {
        return new AcceptancePorts(SpecFiles.fromSystemProperty().declaredPorts());
    }

    @Bean
    public SpringExampleWorld exampleWorld(AcceptanceClock clock, AcceptancePorts ports) {
        return new SpringExampleWorld(clock, ports);
    }
}
