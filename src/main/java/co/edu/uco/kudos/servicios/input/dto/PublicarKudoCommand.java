package co.edu.uco.kudos.servicios.input.dto;

import java.util.List;
import java.util.UUID;

/**
 * autorId sale del JWT (nunca del cuerpo de la petición) y traceId del filtro de trazas.
 */
public record PublicarKudoCommand(
        UUID autorId,
        UUID destinatarioId,
        UUID categoriaId,
        String contenido,
        List<ArchivoAdjunto> adjuntos,
        String traceId) {
}
