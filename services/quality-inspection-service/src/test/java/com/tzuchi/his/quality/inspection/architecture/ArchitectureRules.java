package com.tzuchi.his.quality.inspection.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 架構規則的定義（未凍結）。
 *
 * <p>出處：{@code docs/harness/03-harness-plan.md} 第六節「第一批 ArchUnit 規則」、
 * {@code design-implementation-gap-2026-11-11.md} §3.7、
 * {@code services/quality-inspection-service/README.md} §6.1。
 *
 * <p>規則的描述文字（{@code as(...)}）是凍結基線的索引鍵，改動後要重建該規則的基線。
 */
public final class ArchitectureRules {

    public static final String ROOT = "com.tzuchi.his.quality.inspection";

    private static final String DOMAIN = ROOT + ".domain..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String INTERFACES = ROOT + ".interfaces..";
    private static final String INFRASTRUCTURE = ROOT + ".infrastructure..";
    private static final String REPORTING = ROOT + ".reporting..";

    /**
     * 允許的依賴方向：
     * interfaces → application、reporting、domain；
     * infrastructure → application、domain；
     * reporting → application、domain；
     * application → domain；
     * domain 不依賴其他層。
     */
    public static final ArchRule ARCH_01_LAYER_DEPENDENCIES = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("domain").definedBy(DOMAIN)
            .layer("application").definedBy(APPLICATION)
            .layer("interfaces").definedBy(INTERFACES)
            .layer("infrastructure").definedBy(INFRASTRUCTURE)
            .layer("reporting").definedBy(REPORTING)
            .whereLayer("interfaces").mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("reporting").mayOnlyBeAccessedByLayers("interfaces")
            .whereLayer("application").mayOnlyBeAccessedByLayers("interfaces", "infrastructure", "reporting")
            .as("ARCH-01 分層依賴方向");

    public static final ArchRule ARCH_02_NO_SYSTEM_CLOCK_OR_RANDOM_ID = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().callMethodWhere(readsSystemClockOrRandomId())
            .as("ARCH-02 domain 與 application 不得直接取系統時間或產生亂數 id")
            .because("時間與 id 由外部傳入，規則才能重現與測試");

    public static final ArchRule ARCH_03_NO_ILLEGAL_ARGUMENT_EXCEPTION = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().callConstructorWhere(constructs(IllegalArgumentException.class))
            .as("ARCH-03 domain 與 application 不得丟 IllegalArgumentException")
            .because("它沒有對應的錯誤映射，會變成 HTTP 500；改用 domain.common 或 application.common 的例外");

    private static DescribedPredicate<JavaMethodCall> readsSystemClockOrRandomId() {
        return new DescribedPredicate<>("取系統時間或產生亂數 id") {
            @Override
            public boolean test(JavaMethodCall call) {
                String owner = call.getTargetOwner().getName();
                String name = call.getName();
                if (owner.startsWith("java.time.") && name.equals("now")) {
                    // now(Clock) 的時間來源由呼叫端決定，不算
                    return call.getTarget().getRawParameterTypes().stream()
                            .noneMatch(type -> type.isEquivalentTo(java.time.Clock.class));
                }
                return (owner.equals("java.util.UUID") && name.equals("randomUUID"))
                        || (owner.equals("java.lang.System") && name.equals("currentTimeMillis"));
            }
        };
    }

    private static DescribedPredicate<JavaConstructorCall> constructs(Class<?> type) {
        return new DescribedPredicate<>("建立 " + type.getSimpleName()) {
            @Override
            public boolean test(JavaConstructorCall call) {
                JavaClass owner = call.getTargetOwner();
                return owner.isEquivalentTo(type);
            }
        };
    }

    private ArchitectureRules() {
    }
}
