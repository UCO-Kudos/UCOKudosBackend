package co.edu.uco.kudos.arquitectura;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Component;

/**
 * Reglas AR-01..AR-06 de docs/04-arquitectura.md §4.
 * allowEmptyShould(true): mientras un paquete esté vacío la regla no tiene a quién aplicarse
 * y no debe fallar por eso; en cuanto aparezcan clases se verifican normalmente.
 */
@AnalyzeClasses(packages = "co.edu.uco.kudos", importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasArquitecturaTest {

    private static final String DOMINIO = "co.edu.uco.kudos.dominio..";
    private static final String SERVICIOS = "co.edu.uco.kudos.servicios..";
    private static final String INFRAESTRUCTURA = "co.edu.uco.kudos.infraestructura..";

    @ArchTest
    static final ArchRule AR01_dominio_no_depende_de_servicios_ni_infraestructura =
            noClasses().that().resideInAPackage(DOMINIO)
                    .should().dependOnClassesThat().resideInAnyPackage(SERVICIOS, INFRAESTRUCTURA)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule AR02_dominio_solo_usa_java_y_el_propio_dominio =
            classes().that().resideInAPackage(DOMINIO)
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("java.lang..", "java.util..", "java.time..", "java.math..", DOMINIO)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule AR03_servicios_no_depende_de_infraestructura =
            noClasses().that().resideInAPackage(SERVICIOS)
                    .should().dependOnClassesThat().resideInAPackage(INFRAESTRUCTURA)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule AR04_controladores_y_listeners_solo_usan_puertos_de_entrada =
            noClasses().that().resideInAnyPackage(
                            "co.edu.uco.kudos.infraestructura.input.controlador..",
                            "co.edu.uco.kudos.infraestructura.input.evento..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "co.edu.uco.kudos.servicios.input.adapter..",
                            "co.edu.uco.kudos.servicios.output..",
                            "co.edu.uco.kudos.infraestructura.output..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule AR05_casos_de_uso_solo_salen_por_puertos_de_salida =
            classes().that().resideInAPackage("co.edu.uco.kudos.servicios.input.adapter..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            "java..",
                            DOMINIO,
                            SERVICIOS,
                            "org.springframework.stereotype..",
                            "org.springframework.transaction.annotation..",
                            "org.slf4j..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule AR06_adaptadores_de_salida_implementan_puertos_de_salida =
            classes().that().resideInAPackage("co.edu.uco.kudos.infraestructura.output.adapter..")
                    .and().areMetaAnnotatedWith(Component.class)
                    .should().implement(resideInAPackage("co.edu.uco.kudos.servicios.output.port.."))
                    .allowEmptyShould(true);
}
