package co.edu.uco.kudos.infraestructura.input.controlador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Campos del formulario multipart de POST /api/kudos. Aquí solo se valida el formato (400);
 * las reglas de negocio las valida el caso de uso y el dominio (422).
 */
public record PublicarKudoRequest(
        @NotNull UUID destinatarioId,
        @NotNull UUID categoriaId,
        @NotBlank String contenido,
        List<MultipartFile> adjuntos) {
}
