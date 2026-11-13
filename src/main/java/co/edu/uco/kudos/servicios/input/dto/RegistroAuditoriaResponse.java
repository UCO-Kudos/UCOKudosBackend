package co.edu.uco.kudos.servicios.input.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RegistroAuditoriaResponse(
        UUID id,
        Instant ocurridoEn,
        UUID actorId,
        String accion,
        String recursoTipo,
        UUID recursoId,
        String resultado,
        String traceId,
        Map<String, String> detalle) {
}
