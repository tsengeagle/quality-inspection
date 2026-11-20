package com.tzuchi.his.quality.inspection.acceptance;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 埠的替身（D-065 第 11 點）。
 *
 * <ul>
 *   <li>規格的埠 X 對到介面 XPort，操作對到同名的方法。</li>
 *   <li>只有規格（model.yaml 的 ports）宣告過的埠會被替換；其他 bean 原樣返回。</li>
 *   <li>替換只在 spec_version 2 的範例執行期間生效（{@link #activate()} 到 {@link #deactivate()}）；
 *       其他時候呼叫照常交給原本的實作。</li>
 *   <li>生效期間，埠的操作只回傳範例用 returns 設定的值。沒有設定就執行到，丟 AssertionError 並記錄下來：
 *       範例未通過，即使程式把例外吃掉也一樣。</li>
 * </ul>
 *
 * <p>被替換的 bean 換成只實作它那些介面的代理。依實作類別注入這個 bean 的地方會在啟動時失敗：
 * 規格宣告成埠的依賴，程式要依介面注入。</p>
 */
public final class AcceptancePorts implements BeanPostProcessor {

    private static final String SUFFIX = "Port";

    private final Set<String> declared;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Map<String, Class<?>> interfaces = new ConcurrentHashMap<>();
    private final Map<String, Object> returns = new ConcurrentHashMap<>();
    private final List<String> unconfigured = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean active;

    /** 沒有回傳值（void，或 returns 是 null）在對應表裡的記號：ConcurrentHashMap 不能放 null。 */
    private static final Object NOTHING = new Object();

    /** @param declared 規格宣告的埠名稱（不含 Port） */
    public AcceptancePorts(Set<String> declared) {
        this.declared = new TreeSet<>(declared);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> type = ClassUtils.getUserClass(bean);
        Class<?>[] all = ClassUtils.getAllInterfacesForClass(type, type.getClassLoader());
        Map<String, Class<?>> ports = new LinkedHashMap<>();
        for (Class<?> candidate : all) {
            String simple = candidate.getSimpleName();
            if (simple.endsWith(SUFFIX) && simple.length() > SUFFIX.length()) {
                String port = simple.substring(0, simple.length() - SUFFIX.length());
                if (declared.contains(port)) {
                    ports.put(port, candidate);
                }
            }
        }
        if (ports.isEmpty()) {
            return bean;
        }
        for (Map.Entry<String, Class<?>> port : ports.entrySet()) {
            Class<?> previous = interfaces.put(port.getKey(), port.getValue());
            if (previous != null && previous != port.getValue()) {
                throw new IllegalStateException("有兩個以上的介面叫 " + port.getKey() + SUFFIX);
            }
        }
        InvocationHandler handler = (proxy, method, args) -> {
            String port = portOf(method, ports);
            if (port != null && active) {
                String key = port + "." + method.getName();
                Object value = returns.get(key);
                if (value == null) {
                    unconfigured.add(key);
                    throw new AssertionError("執行到沒有設定 returns 的埠：" + key);
                }
                return value == NOTHING ? null : value;
            }
            try {
                return method.invoke(bean, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
        return Proxy.newProxyInstance(type.getClassLoader(), all, handler);
    }

    private static String portOf(Method method, Map<String, Class<?>> ports) {
        for (Map.Entry<String, Class<?>> port : ports.entrySet()) {
            if (method.getDeclaringClass().isAssignableFrom(port.getValue())
                    && method.getDeclaringClass() != Object.class) {
                return port.getKey();
            }
        }
        return null;
    }

    /** 一個 spec_version 2 的範例開始：清掉設定，替換生效。 */
    public void activate() {
        returns.clear();
        unconfigured.clear();
        active = true;
    }

    /** 範例結束：埠回到原本的實作。 */
    public void deactivate() {
        active = false;
        returns.clear();
        unconfigured.clear();
    }

    /** @throws RunnerProblem 找不到埠或操作、回傳值轉不成操作的回傳型別 */
    public void portReturns(String port, String operation, Object value) {
        if (!declared.contains(port)) {
            throw new RunnerProblem("埠 " + port + " 沒有在任何 model.yaml 的 ports 宣告，不能替換");
        }
        Class<?> type = interfaces.get(port);
        if (type == null) {
            throw new RunnerProblem("找不到埠 " + port + "（沒有 bean 實作介面 " + port + SUFFIX + "）");
        }
        List<Method> found = new ArrayList<>();
        for (Method m : type.getMethods()) {
            if (m.getName().equals(operation) && !m.isBridge() && !m.isSynthetic()) {
                found.add(m);
            }
        }
        if (found.size() != 1) {
            throw new RunnerProblem(port + SUFFIX + " 要有剛好一個叫 " + operation + " 的方法，實際有 " + found.size() + " 個");
        }
        Method method = found.get(0);
        Object converted = NOTHING;
        if (value != null && method.getReturnType() != void.class) {
            JavaType target = mapper.getTypeFactory().constructType(method.getGenericReturnType());
            try {
                converted = mapper.convertValue(value, target);
            } catch (IllegalArgumentException e) {
                throw new RunnerProblem(port + "." + operation + " 的 returns 無法轉成 " + target + "：" + e.getMessage());
            }
        }
        returns.put(port + "." + operation, converted);
    }

    public List<String> takeUnconfiguredCalls() {
        synchronized (unconfigured) {
            List<String> out = new ArrayList<>(unconfigured);
            unconfigured.clear();
            return out;
        }
    }
}
