package com.tzuchi.his.quality.inspection.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 違規樣本（P2）：每條架構規則配一組故意違規的類別，證明規則真的會擋。
 *
 * <p>樣本在 {@code samples/} 底下，各自有一套假的分層套件。這裡用與正式閘門相同的規則定義去掃樣本，
 * 不經過凍結基線。規則被改成永遠通過時，這裡的測試會失敗。
 */
class ArchitectureRuleSamplesTest {

    private static final String SAMPLES = ArchitectureRules.ROOT + ".architecture.samples";

    @Test
    void arch01_detectsEachForbiddenDirection() {
        List<String> violations = violations("layering", ArchitectureRules::layerDependencies);

        assertReported(violations, "DomainUsesApplication");
        assertReported(violations, "ApplicationUsesInterfaces");
        assertReported(violations, "ApplicationUsesInfrastructure");
        assertReported(violations, "ReportingUsesInfrastructure");
        assertReported(violations, "InterfacesUsesInfrastructure");
        assertReported(violations, "InfrastructureUsesInterfaces");
        assertReported(violations, "InfrastructureUsesReporting");
        assertNotReported(violations, "AllowedInterfacesUsesApplication");
    }

    @Test
    void arch02_detectsEachForbiddenAccess() {
        List<String> violations = violations("clock", ArchitectureRules::noSystemClockOrRandomId);

        assertReported(violations, "DomainReadsInstantNow");
        assertReported(violations, "DomainGeneratesRandomUuid");
        assertReported(violations, "ApplicationReadsLocalDateTimeNow");
        assertReported(violations, "ApplicationReadsCurrentTimeMillis");
        assertReported(violations, "ApplicationUsesRandomUuidReference");
        assertNotReported(violations, "AllowedReadsInjectedClock");
    }

    @Test
    void arch03_detectsEachForbiddenConstruction() {
        List<String> violations = violations("exception", ArchitectureRules::noIllegalArgumentException);

        assertReported(violations, "DomainThrowsIllegalArgument");
        assertReported(violations, "ApplicationUsesIllegalArgumentReference");
        assertNotReported(violations, "AllowedThrowsIllegalState");
    }

    private static List<String> violations(String sample, Function<String, ArchRule> rule) {
        String root = SAMPLES + "." + sample;
        JavaClasses classes = new ClassFileImporter().importPackages(root);
        assertThat(classes.size()).as("%s 底下的樣本類別數", root).isGreaterThan(0);
        EvaluationResult result = rule.apply(root).evaluate(classes);
        return result.getFailureReport().getDetails();
    }

    private static void assertReported(List<String> violations, String sampleClass) {
        assertThat(violations).as("規則應報出違規樣本 %s", sampleClass)
                .anyMatch(detail -> detail.contains("." + sampleClass + "."));
    }

    private static void assertNotReported(List<String> violations, String sampleClass) {
        assertThat(violations).as("規則不應報出合規對照 %s", sampleClass)
                .noneMatch(detail -> detail.contains("." + sampleClass + "."));
    }
}
