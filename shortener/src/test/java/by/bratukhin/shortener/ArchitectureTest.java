package by.bratukhin.shortener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;

///
/// Architecture test.
///
class ArchitectureTest {

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(
                "by.bratukhin.shortener"
            );
    }

    @Test
    void layerDependencies() {
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy("by.bratukhin.shortener.web..")
            .layer("Service").definedBy("by.bratukhin.shortener.service..")
            .layer("Repository").definedBy("by.bratukhin.shortener.repository..")

            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")

            .check(importedClasses);
    }

    @Test
    void springDataIsNotUsedFromControllers() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("by.bratukhin.shortener.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework.data..");

        rule.check(importedClasses);
    }
}
