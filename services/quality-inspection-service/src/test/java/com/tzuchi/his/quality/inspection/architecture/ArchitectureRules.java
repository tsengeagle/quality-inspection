package com.tzuchi.his.quality.inspection.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorReferenceTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 架構規則的定義（未凍結）。
 *
 * <p>出處：{@code docs/harness/03-harness-plan.md} 第六節「第一批 ArchUnit 規則」、
 * {@code design-implementation-gap-2026-11-11.md} §3.7、
 * {@code services/quality-inspection-service/README.md} §6.1；
 * 依賴方向與 ARCH-05 的依據是 {@code decision-log.md} 的 D-048。
 *
 * <p>規則的描述文字（{@code as(...)}）是凍結基線的索引鍵，改動後要重建該規則的基線。
 */
public final class ArchitectureRules {

    public static final String ROOT = "com.tzuchi.his.quality.inspection";

    public static final ArchRule ARCH_01_LAYER_DEPENDENCIES = layerDependencies(ROOT);

    public static final ArchRule ARCH_02_NO_SYSTEM_CLOCK_OR_RANDOM_ID = noSystemClockOrRandomId(ROOT);

    public static final ArchRule ARCH_03_NO_ILLEGAL_ARGUMENT_EXCEPTION = noIllegalArgumentException(ROOT);

    public static final ArchRule ARCH_05_REPORTING_USES_ONLY_APPLICATION_COMMON = reportingUsesOnlyApplicationCommon(ROOT);

    /**
     * 允許的依賴方向：
     * interfaces → application、reporting、domain；
     * infrastructure → application、domain；
     * reporting → application、domain；
     * application → domain；
     * domain 不依賴其他層。
     *
     * <p>以根套件為參數：正式的閘門傳入 {@link #ROOT}，違規樣本的測試傳入樣本的根套件，
     * 兩者執行同一份規則定義。
     */
    public static ArchRule layerDependencies(String root) {
        return layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("domain").definedBy(root + ".domain..")
                .layer("application").definedBy(root + ".application..")
                .layer("interfaces").definedBy(root + ".interfaces..")
                .layer("infrastructure").definedBy(root + ".infrastructure..")
                .layer("reporting").definedBy(root + ".reporting..")
                .whereLayer("interfaces").mayNotBeAccessedByAnyLayer()
                .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer()
                .whereLayer("reporting").mayOnlyBeAccessedByLayers("interfaces")
                .whereLayer("application").mayOnlyBeAccessedByLayers("interfaces", "infrastructure", "reporting")
                .as("ARCH-01 分層依賴方向");
    }

    public static ArchRule noSystemClockOrRandomId(String root) {
        return noClasses()
                .that().resideInAnyPackage(root + ".domain..", root + ".application..")
                .should().accessTargetWhere(readsSystemClockOrRandomId())
                .as("ARCH-02 domain 與 application 不得直接取系統時間或產生亂數 id")
                .because("時間與 id 由外部傳入，規則才能重現與測試");
    }

    public static ArchRule noIllegalArgumentException(String root) {
        return noClasses()
                .that().resideInAnyPackage(root + ".domain..", root + ".application..")
                .should().accessTargetWhere(constructs(IllegalArgumentException.class))
                .as("ARCH-03 domain 與 application 不得丟 IllegalArgumentException")
                .because("它沒有對應的錯誤映射，會變成 HTTP 500；改用 domain.common 或 application.common 的例外");
    }

    /**
     * D-048 的過渡規則：reporting 搬進 application 的查詢側之前，對 application 只能依賴 application.common。
     * 目前沒有既有違規，所以不經過凍結基線。
     */
    public static ArchRule reportingUsesOnlyApplicationCommon(String root) {
        return noClasses()
                .that().resideInAPackage(root + ".reporting..")
                .should().dependOnClassesThat(
                        JavaClass.Predicates.resideInAPackage(root + ".application..")
                                .and(DescribedPredicate.not(
                                        JavaClass.Predicates.resideInAPackage(root + ".application.common.."))))
                .as("ARCH-05 reporting 對 application 只能依賴 application.common")
                .because("D-048：報表不得呼叫命令 use case；reporting 之後會併入 application 的查詢側");
    }

    // 兩個判斷式都看「存取」而不只看「呼叫」，方法參考（UUID::randomUUID、IllegalArgumentException::new）也算。

    private static DescribedPredicate<JavaAccess<?>> readsSystemClockOrRandomId() {
        return new DescribedPredicate<>("取系統時間或產生亂數 id") {
            @Override
            public boolean test(JavaAccess<?> access) {
                if (!(access.getTarget() instanceof CodeUnitAccessTarget)) {
                    return false;
                }
                CodeUnitAccessTarget target = (CodeUnitAccessTarget) access.getTarget();
                String owner = access.getTargetOwner().getName();
                String name = access.getName();
                if (owner.startsWith("java.time.") && name.equals("now")) {
                    // now(Clock) 的時間來源由呼叫端決定，不算
                    return target.getRawParameterTypes().stream()
                            .noneMatch(type -> type.isEquivalentTo(java.time.Clock.class));
                }
                return (owner.equals("java.util.UUID") && name.equals("randomUUID"))
                        || (owner.equals("java.lang.System") && name.equals("currentTimeMillis"));
            }
        };
    }

    private static DescribedPredicate<JavaAccess<?>> constructs(Class<?> type) {
        return new DescribedPredicate<>("建立 " + type.getSimpleName()) {
            @Override
            public boolean test(JavaAccess<?> access) {
                return access.getTarget() instanceof ConstructorCallTarget
                        || access.getTarget() instanceof ConstructorReferenceTarget
                        ? access.getTargetOwner().isEquivalentTo(type)
                        : false;
            }
        };
    }

    private ArchitectureRules() {
    }
}
