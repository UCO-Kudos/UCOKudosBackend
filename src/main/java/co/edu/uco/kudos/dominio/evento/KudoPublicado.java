package co.edu.uco.kudos.dominio.evento;

import java.util.UUID;

/**
 * Evento de dominio: un Kudo quedó guardado. Lleva el traceId para que las
 * notificaciones (en otro hilo) queden auditadas con el mismo identificador.
 */
public record KudoPublicado(UUID kudoId, UUID autorId, UUID destinatarioId, String traceId) {
}
