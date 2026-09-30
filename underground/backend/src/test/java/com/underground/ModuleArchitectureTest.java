package com.underground;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.underground")
class ModuleArchitectureTest {
    @ArchTest static final ArchRule profilesUseOnlyIdentityApi = noClasses()
        .that().resideInAPackage("..profiles..")
        .should().dependOnClassesThat().resideInAPackage("..identity") ;
    @ArchTest static final ArchRule identityDoesNotDependOnProfiles = noClasses()
        .that().resideInAPackage("..identity..")
        .should().dependOnClassesThat().resideInAPackage("..profiles..");
}
