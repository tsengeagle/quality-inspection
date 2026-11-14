package com.tzuchi.his.quality.inspection.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * 對正式程式執行架構規則。既有違規凍結在 {@code src/test/resources/archunit_store/}，只擋新增的。
 */
@AnalyzeClasses(packages = ArchitectureRules.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule arch01LayerDependencies = freeze(ArchitectureRules.ARCH_01_LAYER_DEPENDENCIES);

    @ArchTest
    static final ArchRule arch02NoSystemClockOrRandomId = freeze(ArchitectureRules.ARCH_02_NO_SYSTEM_CLOCK_OR_RANDOM_ID);

    @ArchTest
    static final ArchRule arch03NoIllegalArgumentException = freeze(ArchitectureRules.ARCH_03_NO_ILLEGAL_ARGUMENT_EXCEPTION);

    /** 沒有既有違規，不凍結。 */
    @ArchTest
    static final ArchRule arch05ReportingUsesOnlyApplicationCommon =
            ArchitectureRules.ARCH_05_REPORTING_USES_ONLY_APPLICATION_COMMON;
}
