package com.tzuchi.his.quality.harness;

import static com.tzuchi.his.quality.harness.ArchitectureRules.PRODUCTION_ROOT;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * 架構閘門：對產品程式執行 {@link ArchitectureRules}。
 *
 * <p>既有違規凍結在 harness/archunit-store；這裡只擋新增的違規。
 */
@AnalyzeClasses(packages = PRODUCTION_ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureGateTest {

    /** 掃描範圍的下限。掃到的類別太少代表範圍設錯，不能算通過。 */
    private static final int MINIMUM_SCANNED_CLASSES = 200;

    @ArchTest
    static final ArchRule layerDependencies =
            FreezingArchRule.freeze(ArchitectureRules.layerDependencies(PRODUCTION_ROOT));

    @ArchTest
    static final ArchRule noSystemClockOrRandomId =
            FreezingArchRule.freeze(ArchitectureRules.noSystemClockOrRandomId(PRODUCTION_ROOT));

    @ArchTest
    static final ArchRule noIllegalArgumentException =
            FreezingArchRule.freeze(ArchitectureRules.noIllegalArgumentException(PRODUCTION_ROOT));

    @ArchTest
    static void scanScopeIsNotEmpty(JavaClasses classes) {
        assertThat(classes.size())
                .as("number of production classes scanned by the architecture gate")
                .isGreaterThanOrEqualTo(MINIMUM_SCANNED_CLASSES);
        for (String layer : new String[] {"domain", "application", "interfaces", "infrastructure", "reporting"}) {
            assertThat(classes.stream().anyMatch(c -> c.getPackageName().startsWith(PRODUCTION_ROOT + "." + layer)))
                    .as("layer '%s' has at least one scanned class", layer)
                    .isTrue();
        }
    }
}
