package co.edu.uco.kudos.servicios.input.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicarKudoResponse(
        UUID id,
        UUID destinatarioId,
        UUID categoriaId,
        Instant creadoEn,
        int adjuntos,
        String traceId) {
}
