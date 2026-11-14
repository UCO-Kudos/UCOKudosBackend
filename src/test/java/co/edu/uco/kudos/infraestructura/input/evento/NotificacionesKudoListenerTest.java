package co.edu.uco.kudos.infraestructura.input.evento;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.infraestructura.configuracion.AsincroniaConfig;
import co.edu.uco.kudos.infraestructura.configuracion.TraceIdFilter;
import co.edu.uco.kudos.servicios.input.port.NotificarKudoPublicadoUseCase;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Prueba con Spring real (sin BD) que el listener solo se ejecuta tras el COMMIT y que una falla
 * en las notificaciones no llega a quien publicó: el Kudo no se revierte.
 * Se usa un gestor de transacciones falso y un ejecutor síncrono para que la prueba sea determinista.
 */
@SpringJUnitConfig(NotificacionesKudoListenerTest.Configuracion.class)
class NotificacionesKudoListenerTest {

    @Autowired
    private ApplicationEventPublisher publicador;
    @Autowired
    private TransactionTemplate transaccion;
    @Autowired
    private CasoDeUsoFalso casoDeUso;

    private final KudoPublicado evento = new KudoPublicado(UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "trace-42");

    @BeforeEach
    void limpiar() {
        casoDeUso.recibidos.clear();
        casoDeUso.traceIdsVistos.clear();
        casoDeUso.fallar = false;
    }

    @Test
    void notificaSoloDespuesDelCommitYConElTraceIdEnElMdc() {
        transaccion.executeWithoutResult(estado -> {
            publicador.publishEvent(evento);
            assertTrue(casoDeUso.recibidos.isEmpty(), "No debe notificar antes del COMMIT");
        });

        assertEquals(List.of(evento), casoDeUso.recibidos);
        assertEquals(List.of("trace-42"), casoDeUso.traceIdsVistos);
        assertNull(MDC.get(TraceIdFilter.CLAVE_MDC), "El MDC se limpia al terminar");
    }

    @Test
    void siLaTransaccionSeRevierteNoSeNotifica() {
        transaccion.executeWithoutResult(estado -> {
            publicador.publishEvent(evento);
            estado.setRollbackOnly();
        });

        assertTrue(casoDeUso.recibidos.isEmpty());
    }

    @Test
    void unaFallaAlNotificarNoLlegaAQuienPublico() {
        casoDeUso.fallar = true;

        assertDoesNotThrow(() -> transaccion.executeWithoutResult(estado -> publicador.publishEvent(evento)));
        assertEquals(List.of(evento), casoDeUso.recibidos);
    }

    static class CasoDeUsoFalso implements NotificarKudoPublicadoUseCase {
        final List<KudoPublicado> recibidos = new CopyOnWriteArrayList<>();
        final List<String> traceIdsVistos = new CopyOnWriteArrayList<>();
        volatile boolean fallar;

        @Override
        public void notificar(KudoPublicado evento) {
            recibidos.add(evento);
            traceIdsVistos.add(MDC.get(TraceIdFilter.CLAVE_MDC));
            if (fallar) {
                throw new IllegalStateException("Brevo y Twilio caídos");
            }
        }
    }

    /** Gestor de transacciones mínimo: no toca ninguna BD, pero dispara las fases COMMIT/ROLLBACK. */
    static class TransaccionesFalsas extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaccion, TransactionDefinition definicion) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus estado) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus estado) {
        }
    }

    @Configuration
    @EnableAsync
    @EnableTransactionManagement
    static class Configuracion {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TransaccionesFalsas();
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager gestor) {
            return new TransactionTemplate(gestor);
        }

        @Bean(name = AsincroniaConfig.EJECUTOR_NOTIFICACIONES)
        Executor ejecutorNotificaciones() {
            return new SyncTaskExecutor();
        }

        @Bean
        CasoDeUsoFalso casoDeUso() {
            return new CasoDeUsoFalso();
        }

        @Bean
        NotificacionesKudoListener listener(CasoDeUsoFalso casoDeUso) {
            return new NotificacionesKudoListener(casoDeUso);
        }
    }
}
