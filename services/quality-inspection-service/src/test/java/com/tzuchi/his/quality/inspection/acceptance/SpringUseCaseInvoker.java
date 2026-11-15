package com.tzuchi.his.quality.inspection.acceptance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 依命名慣例呼叫 use case，不維護對照表。
 *
 * <ul>
 *   <li>規格的 use case 名稱 X 對應 Spring bean，類別名稱是 XUseCase，方法是唯一一個公開的 execute。</li>
 *   <li>execute 的 {@link ActorContext} 參數由範例的 actor 建立；其他參數依<strong>參數名稱</strong>
 *       取規格的輸入（編譯時要有 -parameters）。規格沒給的參數傳 null。</li>
 *   <li>規格的輸入名稱在 execute 裡找不到同名參數，範例無法執行。</li>
 *   <li>被拒絕的錯誤代碼取自例外的 getErrorCode()；沒有這個方法，就是「被拒絕但沒有錯誤代碼」。</li>
 *   <li>輸出依屬性名稱比對（Jackson 轉換 getter）。</li>
 * </ul>
 *
 * <p>名稱對不上時範例會失敗，修正的方向是讓程式使用規格的詞彙，而不是在這裡加對照。</p>
 */
public final class SpringUseCaseInvoker implements UseCaseInvoker {

    private static final String SUFFIX = "UseCase";

    private final ApplicationContext context;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Map<String, String> beanNames = new HashMap<>();
    private final Set<String> duplicated = new LinkedHashSet<>();

    public SpringUseCaseInvoker(ApplicationContext context) {
        this.context = context;
        for (String name : context.getBeanDefinitionNames()) {
            Class<?> type = context.getType(name);
            if (type == null) {
                continue;
            }
            String simple = ClassUtils.getUserClass(type).getSimpleName();
            if (simple.endsWith(SUFFIX) && simple.length() > SUFFIX.length()) {
                String useCase = simple.substring(0, simple.length() - SUFFIX.length());
                if (beanNames.put(useCase, name) != null) {
                    duplicated.add(useCase);
                }
            }
        }
    }

    @Override
    public Outcome invoke(String useCase, Map<String, Object> actor, Map<String, Object> input) {
        if (duplicated.contains(useCase)) {
            throw new RunnerProblem("有兩個以上的類別叫 " + useCase + SUFFIX);
        }
        String beanName = beanNames.get(useCase);
        if (beanName == null) {
            throw new RunnerProblem("找不到 use case " + useCase + "（類別 " + useCase + SUFFIX + "）");
        }
        Object bean = context.getBean(beanName);
        Method method = executeMethod(useCase, ClassUtils.getUserClass(bean));
        Parameter[] parameters = method.getParameters();

        Set<String> unused = new LinkedHashSet<>(input.keySet());
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            if (p.getType() == ActorContext.class) {
                args[i] = actorContext(actor);
                continue;
            }
            if (!p.isNamePresent()) {
                throw new RunnerProblem(useCase + SUFFIX + ".execute 的參數沒有名稱；編譯時要加 -parameters");
            }
            String name = p.getName();
            unused.remove(name);
            if (!input.containsKey(name) || input.get(name) == null) {
                if (p.getType().isPrimitive()) {
                    throw new RunnerProblem(useCase + " 的輸入沒有給 " + name + "，而它是基本型別，不能是 null");
                }
                continue;
            }
            JavaType type = mapper.getTypeFactory().constructType(p.getParameterizedType());
            try {
                args[i] = mapper.convertValue(input.get(name), type);
            } catch (IllegalArgumentException e) {
                throw new RunnerProblem(useCase + " 的輸入 " + name + "=" + input.get(name) + " 無法轉成 "
                        + type + "：" + e.getMessage());
            }
        }
        if (!unused.isEmpty()) {
            throw new RunnerProblem(useCase + SUFFIX + ".execute 沒有這些參數：" + String.join("、", unused)
                    + "（現有參數：" + parameterNames(parameters) + "）");
        }

        try {
            return Outcome.success(method.invoke(bean, args));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            return Outcome.rejected(errorCode(cause), cause.getClass().getSimpleName() + ": " + cause.getMessage());
        } catch (IllegalAccessException e) {
            throw new RunnerProblem("無法呼叫 " + useCase + SUFFIX + ".execute：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> properties(Object value) {
        if (value == null || value instanceof CharSequence || value instanceof Number
                || value instanceof Boolean || value instanceof Enum) {
            throw new RunnerProblem("輸出是單一值（" + value + "），沒有欄位可以比對");
        }
        return mapper.convertValue(value, new TypeReference<Map<String, Object>>() { });
    }

    private static Method executeMethod(String useCase, Class<?> type) {
        List<Method> found = new ArrayList<>();
        for (Method m : type.getMethods()) {
            if (m.getName().equals("execute") && !m.isBridge() && !m.isSynthetic()) {
                found.add(m);
            }
        }
        if (found.size() != 1) {
            throw new RunnerProblem(useCase + SUFFIX + " 要有剛好一個公開的 execute 方法，實際有 " + found.size() + " 個");
        }
        return found.get(0);
    }

    private static ActorContext actorContext(Map<String, Object> actor) {
        if (actor.isEmpty()) {
            throw new RunnerProblem("步驟沒有指定 as，或 actors 裡沒有這個操作者");
        }
        String user = String.valueOf(actor.get("user"));
        Set<DomainRole> roles = EnumSet.noneOf(DomainRole.class);
        Object declared = actor.get("roles");
        if (declared instanceof List) {
            for (Object role : (List<?>) declared) {
                try {
                    roles.add(DomainRole.valueOf(String.valueOf(role)));
                } catch (IllegalArgumentException e) {
                    throw new RunnerProblem("程式沒有角色 " + role);
                }
            }
        }
        Object hospital = actor.get("hospital");
        return new ActorContext(user, user, roles, Optional.ofNullable(hospital == null ? null : String.valueOf(hospital)));
    }

    private static String errorCode(Throwable cause) {
        try {
            Method getter = cause.getClass().getMethod("getErrorCode");
            Object code = getter.invoke(cause);
            return code == null ? null : String.valueOf(code);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String parameterNames(Parameter[] parameters) {
        List<String> names = new ArrayList<>();
        for (Parameter p : parameters) {
            if (p.getType() != ActorContext.class) {
                names.add(p.getName());
            }
        }
        return names.isEmpty() ? "無" : String.join("、", names);
    }
}
