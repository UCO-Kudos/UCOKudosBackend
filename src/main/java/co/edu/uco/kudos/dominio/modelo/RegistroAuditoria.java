package co.edu.uco.kudos.dominio.modelo;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Fila de la auditoría (solo inserción). actorId y recursoId pueden ser nulos,
 * p. ej. un login fallido no tiene actor conocido. detalle lleva solo datos no sensibles.
 */
public record RegistroAuditoria(
        UUID id,
        Instant ocurridoEn,
        UUID actorId,
        AccionAuditoria accion,
        String recursoTipo,
        UUID recursoId,
        ResultadoAuditoria resultado,
        String traceId,
        Map<String, String> detalle) {

    public static RegistroAuditoria nuevo(UUID actorId, AccionAuditoria accion, String recursoTipo, UUID recursoId,
                                          ResultadoAuditoria resultado, String traceId, Map<String, String> detalle) {
        return new RegistroAuditoria(UUID.randomUUID(), Instant.now(), actorId, accion, recursoTipo,
                recursoId, resultado, traceId, detalle);
    }
}
