package com.company.portal.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces a small set of architecture invariants that are cheap to check and
 * easy to keep green. Rules deliberately lenient at platform-init time; each
 * feature module is expected to add its own module-specific rules.
 */
@AnalyzeClasses(
        packages = "com.company.portal",
        importOptions = { ImportOption.DoNotIncludeTests.class }
)
public class ModuleArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_not_access_repositories = noClasses()
            .that().resideInAPackage("..web..").or().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..persistence.repository..")
            .as("Controllers must not access repositories directly — go through application services.")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers_should_not_use_entity_manager = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .as("Controllers must not depend on the JPA EntityManager.")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule infrastructure_should_not_be_accessed_from_other_modules_application = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..")
            .andShould().resideOutsideOfPackages("com.company.portal.shared..")
            .as("Cross-module application code must not reach into another module's infrastructure package.")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule shared_module_must_not_depend_on_feature_modules = noClasses()
            .that().resideInAPackage("com.company.portal.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.company.portal.identity..",
                    "com.company.portal.mfa..",
                    "com.company.portal.audit..",
                    "com.company.portal.notification..")
            .as("The shared platform module must never depend on feature modules.")
            .allowEmptyShould(true);
}
