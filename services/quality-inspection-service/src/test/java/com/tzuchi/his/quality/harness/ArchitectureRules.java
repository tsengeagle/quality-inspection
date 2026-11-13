package com.tzuchi.his.quality.harness;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 架構規則的定義。規則目錄（擋什麼、不擋什麼、出處）見 harness/README.md。
 *
 * <p>每條規則以根套件為參數：正式的閘門傳入產品程式的根套件，
 * 違規樣本的測試傳入樣本的根套件。兩者執行的是同一份規則定義。
 *
 * <p>規則的描述文字是凍結基線的索引鍵，修改描述等於丟掉該規則的基線。
 */
public final class ArchitectureRules {

    public static final String PRODUCTION_ROOT = "com.tzuchi.his.quality.inspection";

    private static final Set<String> NO_ARG_CLOCK_OWNERS = Set.of(
            Instant.class.getName(),
            LocalDate.class.getName(),
            LocalDateTime.class.getName(),
            OffsetDateTime.class.getName(),
            ZonedDateTime.class.getName());

    private ArchitectureRules() {
    }

    /** QI-ARCH-001：分層依賴方向。 */
    public static ArchRule layerDependencies(String root) {
        return layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("domain").definedBy(root + ".domain..")
                .layer("application").definedBy(root + ".application..")
                .layer("reporting").definedBy(root + ".reporting..")
                .layer("interfaces").definedBy(root + ".interfaces..")
                .layer("infrastructure").definedBy(root + ".infrastructure..")
                .whereLayer("domain").mayNotAccessAnyLayer()
                .whereLayer("application").mayOnlyAccessLayers("domain")
                .whereLayer("reporting").mayOnlyAccessLayers("domain", "application")
                .whereLayer("interfaces").mayOnlyAccessLayers("domain", "application", "reporting")
                .whereLayer("infrastructure").mayOnlyAccessLayers("domain", "application", "reporting")
                .as("QI-ARCH-001 layer dependency direction");
    }

    /** QI-ARCH-002：domain 與 application 不得直接取系統時間或亂數 id。 */
    public static ArchRule noSystemClockOrRandomId(String root) {
        return noClasses()
                .that().resideInAnyPackage(root + ".domain..", root + ".application..")
                .should().callMethodWhere(systemClockOrRandomId())
                .as("QI-ARCH-002 domain and application must not read the system clock or generate random ids");
    }

    /** QI-ARCH-003：domain 與 application 不得建立 IllegalArgumentException。 */
    public static ArchRule noIllegalArgumentException(String root) {
        return noClasses()
                .that().resideInAnyPackage(root + ".domain..", root + ".application..")
                .should().callConstructorWhere(constructsIllegalArgumentException())
                .as("QI-ARCH-003 domain and application must not create IllegalArgumentException");
    }

    private static DescribedPredicate<JavaCall<?>> systemClockOrRandomId() {
        return describe("target is a no-arg now(), System.currentTimeMillis() or UUID.randomUUID()", call -> {
            String owner = call.getTargetOwner().getName();
            String name = call.getName();
            boolean noArgs = call.getTarget().getRawParameterTypes().isEmpty();
            return (noArgs && name.equals("now") && NO_ARG_CLOCK_OWNERS.contains(owner))
                    || (name.equals("currentTimeMillis") && owner.equals(System.class.getName()))
                    || (name.equals("randomUUID") && owner.equals(UUID.class.getName()));
        });
    }

    private static DescribedPredicate<JavaConstructorCall> constructsIllegalArgumentException() {
        return describe("target is an IllegalArgumentException constructor",
                call -> call.getTargetOwner().isAssignableTo(IllegalArgumentException.class));
    }
}
