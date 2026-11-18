package com.tzuchi.his.quality.inspection.acceptance.http;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 在每一個 Spring 測試 context 登記一個 {@link MockMvcBuilderCustomizer}，讓自動設定的 MockMvc
 * 在每次請求後把結果交給 {@link HttpObservationRecorder}（B-1：觀察）。
 *
 * <p>以 {@code src/test/resources/application-test.yml} 的 {@code context.initializer.classes} 啟用
 * （Spring Boot 的 DelegatingApplicationContextInitializer 讀這個屬性）。該檔與本類別都是律法（gate），
 * 建構者的測試不需要、也不能選擇加入。</p>
 */
public class HttpObservationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        HttpObservationRecorder.reportOnExit();
        MockMvcBuilderCustomizer customizer = builder -> builder.alwaysDo(HttpObservationRecorder::record);
        context.getBeanFactory().registerSingleton("httpContractObservationCustomizer", customizer);
    }
}
