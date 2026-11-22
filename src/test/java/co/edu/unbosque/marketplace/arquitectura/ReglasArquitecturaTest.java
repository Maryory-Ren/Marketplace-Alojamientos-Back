package co.edu.unbosque.marketplace.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "co.edu.unbosque.marketplace", importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasArquitecturaTest {
    @ArchTest
    static final ArchRule dominio_no_depende_de_capas_externas = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..", "..presentation..");

    @ArchTest
    static final ArchRule aplicacion_no_depende_de_presentacion_ni_infraestructura = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..presentation..", "..infrastructure..");
}
