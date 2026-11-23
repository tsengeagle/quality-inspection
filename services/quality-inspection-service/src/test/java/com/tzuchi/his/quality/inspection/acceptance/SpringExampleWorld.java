package com.tzuchi.his.quality.inspection.acceptance;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.springframework.context.ApplicationListener;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ExampleWorld} 接到應用程式的實作。
 *
 * <ul>
 *   <li>時鐘：{@link AcceptanceClock}，在測試的 context 裡取代應用程式的 Clock。</li>
 *   <li>事件：聽所有 {@link DomainEvent}。事件 X 對到類別 XEvent；屬性取自 getter，
 *       不含 Spring 事件本身的 source 與 timestamp。</li>
 *   <li>埠：{@link AcceptancePorts}。</li>
 * </ul>
 */
public final class SpringExampleWorld implements ExampleWorld, ApplicationListener<DomainEvent> {

    private static final String EVENT_SUFFIX = "Event";
    /** ApplicationEvent 與 Object 的屬性，不是事件的內容。 */
    private static final Set<String> NOT_EVENT_CONTENT = new HashSet<>(Arrays.asList("class", "source", "timestamp"));

    private final AcceptanceClock clock;
    private final AcceptancePorts ports;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final List<EmittedEvent> events = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean active;

    public SpringExampleWorld(AcceptanceClock clock, AcceptancePorts ports) {
        this.clock = clock;
        this.ports = ports;
    }

    @Override
    public void begin(Instant clockStart, ZoneId zone) {
        clock.fix(clockStart, zone);
        ports.activate();
        events.clear();
        active = true;
    }

    @Override
    public void end() {
        active = false;
        events.clear();
        ports.deactivate();
        clock.release();
    }

    @Override
    public void advance(Duration amount) {
        clock.advance(amount);
    }

    @Override
    public void portReturns(String port, String operation, Object returns) {
        ports.portReturns(port, operation, returns);
    }

    @Override
    public List<EmittedEvent> takeEvents() {
        synchronized (events) {
            List<EmittedEvent> out = new ArrayList<>(events);
            events.clear();
            return out;
        }
    }

    @Override
    public List<String> takeUnconfiguredPortCalls() {
        return ports.takeUnconfiguredCalls();
    }

    @Override
    public Object tree(Object value) {
        return value == null ? null : mapper.convertValue(value, Object.class);
    }

    @Override
    public void onApplicationEvent(DomainEvent event) {
        if (!active) {
            return;
        }
        String simple = event.getClass().getSimpleName();
        String name = simple.endsWith(EVENT_SUFFIX) && simple.length() > EVENT_SUFFIX.length()
                ? simple.substring(0, simple.length() - EVENT_SUFFIX.length()) : simple;
        events.add(new EmittedEvent(name, properties(event)));
    }

    private Map<String, Object> properties(Object event) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            for (PropertyDescriptor property : Introspector.getBeanInfo(event.getClass()).getPropertyDescriptors()) {
                Method getter = property.getReadMethod();
                if (getter == null || NOT_EVENT_CONTENT.contains(property.getName())) {
                    continue;
                }
                out.put(property.getName(), tree(getter.invoke(event)));
            }
        } catch (IntrospectionException | ReflectiveOperationException | IllegalArgumentException e) {
            throw new RunnerProblem("無法讀取事件 " + event.getClass().getSimpleName() + " 的屬性：" + e);
        }
        return out;
    }
}
