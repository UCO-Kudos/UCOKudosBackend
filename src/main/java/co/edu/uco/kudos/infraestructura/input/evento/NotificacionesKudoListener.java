package co.edu.uco.kudos.infraestructura.input.evento;

import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.infraestructura.configuracion.AsincroniaConfig;
import co.edu.uco.kudos.infraestructura.configuracion.TraceIdFilter;
import co.edu.uco.kudos.servicios.input.port.NotificarKudoPublicadoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Adaptador de entrada del evento KudoPublicado (página 7).
 * AFTER_COMMIT: solo se notifica un Kudo que ya quedó guardado; si la transacción se revierte,
 * el evento se descarta. @Async: corre en el ejecutor de notificaciones, así el 201 no espera
 * a Brevo ni a Twilio y una falla aquí no puede afectar la publicación.
 */
@Component
public class NotificacionesKudoListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesKudoListener.class);

    private final NotificarKudoPublicadoUseCase notificarKudo;

    public NotificacionesKudoListener(NotificarKudoPublicadoUseCase notificarKudo) {
        this.notificarKudo = notificarKudo;
    }

    @Async(AsincroniaConfig.EJECUTOR_NOTIFICACIONES)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alPublicarKudo(KudoPublicado evento) {
        // Otro hilo no hereda el MDC de la petición: se pone el traceId del evento para que salga en los logs.
        MDC.put(TraceIdFilter.CLAVE_MDC, evento.traceId());
        try {
            notificarKudo.notificar(evento);
        } catch (RuntimeException e) {
            log.error("No se pudieron procesar las notificaciones del kudo {}", evento.kudoId(), e);
        } finally {
            MDC.remove(TraceIdFilter.CLAVE_MDC);
        }
    }
}
