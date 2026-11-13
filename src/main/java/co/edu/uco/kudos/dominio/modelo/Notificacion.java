package co.edu.uco.kudos.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado del envío de un Kudo por un canal. ultimoError y enviadaEn pueden ser nulos
 * (no hubo error / no se envió), igual que en la tabla notificacion.
 */
public record Notificacion(
        UUID id,
        UUID kudoId,
        UUID destinatarioId,
        CanalNotificacion canal,
        EstadoNotificacion estado,
        int intentos,
        String ultimoError,
        Instant creadaEn,
        Instant enviadaEn) {

    public static Notificacion nueva(UUID kudoId, UUID destinatarioId, CanalNotificacion canal,
                                     EstadoNotificacion estado, int intentos, String ultimoError) {
        Instant ahora = Instant.now();
        Instant enviadaEn = estado == EstadoNotificacion.ENVIADA ? ahora : null;
        return new Notificacion(UUID.randomUUID(), kudoId, destinatarioId, canal, estado,
                intentos, ultimoError, ahora, enviadaEn);
    }
}
