package co.edu.uco.kudos.servicios.input.adapter;

import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.dominio.excepcion.EnvioFallidoException;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.CanalNotificacion;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.Persona;
import co.edu.uco.kudos.dominio.modelo.RegistroAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.servicios.input.port.NotificarKudoPublicadoUseCase;
import co.edu.uco.kudos.servicios.output.port.AuditLogPort;
import co.edu.uco.kudos.servicios.output.port.EmailSenderPort;
import co.edu.uco.kudos.servicios.output.port.MetricasNotificacionPort;
import co.edu.uco.kudos.servicios.output.port.NotificacionRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.PersonaRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.PushNotifierPort;
import co.edu.uco.kudos.servicios.output.port.WhatsAppSenderPort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Página 7 del diagrama de secuencia. Corre después del COMMIT del Kudo y en otro hilo, así que
 * nada de lo que pase aquí puede deshacer el Kudo. Los tres canales son independientes: cada uno
 * va en su propio try/catch y su resultado se guarda en notificacion y en auditoria con el
 * mismo traceId de la publicación. Sin @Transactional: cada fila se confirma por separado.
 */
@Service
public class NotificarKudoPublicadoService implements NotificarKudoPublicadoUseCase {

    static final String ASUNTO = "Recibiste un Kudo en UCO Kudos";

    private static final Logger log = LoggerFactory.getLogger(NotificarKudoPublicadoService.class);

    private final PersonaRepositoryPort personas;
    private final EmailSenderPort correo;
    private final WhatsAppSenderPort whatsApp;
    private final PushNotifierPort push;
    private final NotificacionRepositoryPort notificaciones;
    private final AuditLogPort auditoria;
    private final MetricasNotificacionPort metricas;

    public NotificarKudoPublicadoService(PersonaRepositoryPort personas, EmailSenderPort correo,
                                         WhatsAppSenderPort whatsApp, PushNotifierPort push,
                                         NotificacionRepositoryPort notificaciones, AuditLogPort auditoria,
                                         MetricasNotificacionPort metricas) {
        this.personas = personas;
        this.correo = correo;
        this.whatsApp = whatsApp;
        this.push = push;
        this.notificaciones = notificaciones;
        this.auditoria = auditoria;
        this.metricas = metricas;
    }

    @Override
    public void notificar(KudoPublicado evento) {
        Optional<Persona> posibleDestinatario = personas.buscarPorId(evento.destinatarioId());
        if (posibleDestinatario.isEmpty()) {
            log.error("No se encontró al destinatario del kudo {}; no se notifica", evento.kudoId());
            return;
        }
        Persona destinatario = posibleDestinatario.get();
        String autor = personas.buscarPorId(evento.autorId()).map(Persona::nombreCompleto).orElse("Alguien");
        // Mensaje fijo, sin el texto del Kudo: a los terceros solo va el aviso (docs/06 §6).
        String mensaje = "Hola " + destinatario.nombres() + ", " + autor
                + " te envió un Kudo en UCO Kudos. Entra a la aplicación para verlo.";

        notificarPorCanal(evento, CanalNotificacion.CORREO,
                () -> enviada(correo.enviar(destinatario.correo(), ASUNTO, mensaje)));
        notificarPorCanal(evento, CanalNotificacion.WHATSAPP,
                () -> enviada(whatsApp.enviar(destinatario.celular(), mensaje)));
        notificarPorCanal(evento, CanalNotificacion.PUSH,
                () -> push.enviar(destinatario.id(), evento.kudoId(), mensaje)
                        ? enviada(1)
                        : new Resultado(EstadoNotificacion.OMITIDA, 0, "El destinatario no está conectado"));
    }

    private void notificarPorCanal(KudoPublicado evento, CanalNotificacion canal, Supplier<Resultado> envio) {
        Resultado resultado;
        try {
            resultado = envio.get();
        } catch (EnvioFallidoException e) {
            resultado = fallido(canal, e.intentos(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error inesperado en el canal {} del kudo {}", canal, evento.kudoId(), e);
            resultado = fallido(canal, 1, "Error inesperado: " + e.getClass().getSimpleName());
        }
        log.info("Notificación {} del kudo {}: {} ({} intentos)", canal, evento.kudoId(), resultado.estado(),
                resultado.intentos());
        metricas.contarNotificacion(canal, resultado.estado());
        registrar(evento, canal, resultado);
    }

    // Si la BD falla al guardar el resultado de un canal, se registra en el log y se sigue con el siguiente.
    private void registrar(KudoPublicado evento, CanalNotificacion canal, Resultado resultado) {
        try {
            Notificacion notificacion = Notificacion.nueva(evento.kudoId(), evento.destinatarioId(), canal,
                    resultado.estado(), resultado.intentos(), resultado.error());
            notificaciones.guardar(notificacion);

            Map<String, String> detalle = new LinkedHashMap<>();
            detalle.put("kudoId", evento.kudoId().toString());
            detalle.put("canal", canal.name());
            detalle.put("intentos", String.valueOf(resultado.intentos()));
            if (resultado.error() != null) {
                detalle.put("error", resultado.error());
            }
            auditoria.registrar(RegistroAuditoria.nuevo(evento.autorId(), accion(canal, resultado.estado()),
                    "NOTIFICACION", notificacion.id(), resultadoAuditoria(resultado.estado()), evento.traceId(),
                    detalle));
        } catch (RuntimeException e) {
            log.error("No se pudo registrar la notificación {} del kudo {}", canal, evento.kudoId(), e);
        }
    }

    static AccionAuditoria accion(CanalNotificacion canal, EstadoNotificacion estado) {
        boolean enviada = estado == EstadoNotificacion.ENVIADA;
        return switch (canal) {
            case CORREO -> enviada ? AccionAuditoria.NOTIFICACION_CORREO_ENVIADA
                    : AccionAuditoria.NOTIFICACION_CORREO_FALLIDA;
            case WHATSAPP -> enviada ? AccionAuditoria.NOTIFICACION_WHATSAPP_ENVIADA
                    : AccionAuditoria.NOTIFICACION_WHATSAPP_FALLIDA;
            case PUSH -> enviada ? AccionAuditoria.NOTIFICACION_PUSH_ENVIADA
                    : AccionAuditoria.NOTIFICACION_PUSH_OMITIDA_DESCONECTADO;
        };
    }

    private static ResultadoAuditoria resultadoAuditoria(EstadoNotificacion estado) {
        return switch (estado) {
            case ENVIADA -> ResultadoAuditoria.EXITO;
            case FALLIDA -> ResultadoAuditoria.FALLO;
            case OMITIDA, PENDIENTE -> ResultadoAuditoria.OMITIDO;
        };
    }

    // El push no tiene estado FALLIDA en el diseño: si no se pudo entregar, queda omitido.
    private static Resultado fallido(CanalNotificacion canal, int intentos, String error) {
        EstadoNotificacion estado = canal == CanalNotificacion.PUSH
                ? EstadoNotificacion.OMITIDA : EstadoNotificacion.FALLIDA;
        return new Resultado(estado, intentos, error);
    }

    private static Resultado enviada(int intentos) {
        return new Resultado(EstadoNotificacion.ENVIADA, intentos, null);
    }

    private record Resultado(EstadoNotificacion estado, int intentos, String error) {
    }
}
