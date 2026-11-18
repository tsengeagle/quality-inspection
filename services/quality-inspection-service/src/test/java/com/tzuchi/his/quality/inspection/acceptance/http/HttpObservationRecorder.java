package com.tzuchi.his.quality.inspection.acceptance.http;

import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 收集整個測試 JVM 裡經過 MockMvc 的每一個 HTTP 交換（B-1：觀察）。
 *
 * <p>由 {@link HttpObservationInitializer} 掛到每一個 Spring 測試 context，建構者的測試不需要、也不能選擇加入。
 * JVM 結束時寫報告（{@link HttpContractReport}），見 {@link #reportOnExit()}。</p>
 */
public final class HttpObservationRecorder {

    private static final List<HttpObservation> OBSERVATIONS = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicBoolean HOOKED = new AtomicBoolean();

    private HttpObservationRecorder() {}

    static void record(MvcResult result) throws Exception {
        String uri = result.getRequest().getRequestURI();
        String query = result.getRequest().getQueryString();
        record(new HttpObservation(result.getRequest().getMethod(), query == null ? uri : uri + "?" + query,
                result.getResponse().getStatus(), result.getResponse().getContentAsString(StandardCharsets.UTF_8)));
    }

    static void record(HttpObservation observation) {
        OBSERVATIONS.add(observation);
        reportOnExit();
    }

    /**
     * 登記 JVM 結束時寫報告（只登記一次）。建立任何 Spring 測試 context 時就會呼叫，
     * 所以即使整個 JVM 沒有任何 HTTP 呼叫，報告也會寫出來，並列出每個端點「沒有觀察到任何成功回應」，
     * 不會留下上一次的舊報告。
     */
    static void reportOnExit() {
        if (HOOKED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(HttpContractReport::writeQuietly, "http-contract-report"));
        }
    }

    static List<HttpObservation> snapshot() {
        synchronized (OBSERVATIONS) {
            return new ArrayList<>(OBSERVATIONS);
        }
    }
}
