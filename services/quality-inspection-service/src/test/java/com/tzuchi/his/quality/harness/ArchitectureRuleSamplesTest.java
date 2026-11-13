package com.tzuchi.his.quality.harness;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * 違規樣本（P2）：每條架構規則配一組故意違規的類別，證明規則真的會擋。
 *
 * <p>若有人把規則改成永遠通過，這裡的測試會失敗。樣本不經過凍結基線。
 */
class ArchitectureRuleSamplesTest {

    private static final String SAMPLES = "com.tzuchi.his.quality.harness.samples";

    @Test
    void layerDependencies_detectsEachForbiddenDirection() {
        EvaluationResult result = evaluate("layering", ArchitectureRules::layerDependencies);

        assertViolationMentions(result, "DomainUsesApplication");
        assertViolationMentions(result, "ApplicationUsesInterfaces");
        assertViolationMentions(result, "ApplicationUsesInfrastructure");
        assertViolationMentions(result, "ReportingUsesInfrastructure");
        assertViolationMentions(result, "InterfacesUsesInfrastructure");
        assertViolationMentions(result, "InfrastructureUsesInterfaces");
        assertNoViolationMentions(result, "AllowedInterfacesUsesApplication");
    }

    @Test
    void noSystemClockOrRandomId_detectsEachForbiddenCall() {
        EvaluationResult result = evaluate("clock", ArchitectureRules::noSystemClockOrRandomId);

        assertViolationMentions(result, "DomainReadsInstantNow");
        assertViolationMentions(result, "DomainGeneratesRandomUuid");
        assertViolationMentions(result, "ApplicationReadsLocalDateTimeNow");
        assertViolationMentions(result, "ApplicationReadsCurrentTimeMillis");
        assertNoViolationMentions(result, "AllowedReadsInjectedClock");
    }

    @Test
    void noIllegalArgumentException_detectsEachForbiddenConstruction() {
        EvaluationResult result = evaluate("exception", ArchitectureRules::noIllegalArgumentException);

        assertViolationMentions(result, "DomainThrowsIllegalArgument");
        assertViolationMentions(result, "ApplicationThrowsIllegalArgumentSubclass");
        assertNoViolationMentions(result, "AllowedThrowsIllegalState");
    }

    private static EvaluationResult evaluate(String sample, Function<String, ArchRule> rule) {
        String root = SAMPLES + "." + sample;
        JavaClasses classes = new ClassFileImporter().importPackages(root);
        assertThat(classes.size()).as("sample classes under %s", root).isGreaterThan(0);
        return rule.apply(root).evaluate(classes);
    }

    private static void assertViolationMentions(EvaluationResult result, String className) {
        assertThat(result.getFailureReport().getDetails())
                .as("violations reported by the rule")
                .anyMatch(detail -> detail.contains(className));
    }

    private static void assertNoViolationMentions(EvaluationResult result, String className) {
        assertThat(result.getFailureReport().getDetails())
                .as("violations reported by the rule")
                .noneMatch(detail -> detail.contains(className + "."));
    }
}
