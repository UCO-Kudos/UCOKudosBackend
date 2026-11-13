package co.edu.uco.kudos.infraestructura.input.controlador;

import co.edu.uco.kudos.infraestructura.configuracion.TraceIdFilter;
import co.edu.uco.kudos.servicios.input.dto.ArchivoAdjunto;
import co.edu.uco.kudos.servicios.input.dto.PublicarKudoCommand;
import co.edu.uco.kudos.servicios.input.dto.PublicarKudoResponse;
import co.edu.uco.kudos.servicios.input.port.PublicarKudoUseCase;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/kudos")
public class KudoController {

    private final PublicarKudoUseCase publicarKudo;

    public KudoController(PublicarKudoUseCase publicarKudo) {
        this.publicarKudo = publicarKudo;
    }

    /** El autor es siempre el usuario del JWT: el cliente no puede publicar a nombre de otro. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicarKudoResponse> publicar(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @ModelAttribute PublicarKudoRequest solicitud)
            throws IOException {
        PublicarKudoCommand comando = new PublicarKudoCommand(
                UUID.fromString(jwt.getSubject()),
                solicitud.destinatarioId(),
                solicitud.categoriaId(),
                solicitud.contenido(),
                aArchivos(solicitud.adjuntos()),
                TraceIdFilter.actual());
        return ResponseEntity.status(HttpStatus.CREATED).body(publicarKudo.publicar(comando));
    }

    // Un campo de archivo vacío (el usuario no eligió nada) no cuenta como adjunto.
    private static List<ArchivoAdjunto> aArchivos(List<MultipartFile> adjuntos) throws IOException {
        List<ArchivoAdjunto> archivos = new ArrayList<>();
        if (adjuntos == null) {
            return archivos;
        }
        for (MultipartFile adjunto : adjuntos) {
            if (!adjunto.isEmpty()) {
                archivos.add(new ArchivoAdjunto(adjunto.getOriginalFilename(), adjunto.getContentType(),
                        adjunto.getBytes()));
            }
        }
        return archivos;
    }
}
