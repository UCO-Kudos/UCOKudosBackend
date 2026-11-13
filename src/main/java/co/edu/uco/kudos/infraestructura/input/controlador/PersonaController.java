package co.edu.uco.kudos.infraestructura.input.controlador;

import co.edu.uco.kudos.servicios.input.dto.DestinatarioResponse;
import co.edu.uco.kudos.servicios.input.port.ListarDestinatariosUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final ListarDestinatariosUseCase listarDestinatarios;

    public PersonaController(ListarDestinatariosUseCase listarDestinatarios) {
        this.listarDestinatarios = listarDestinatarios;
    }

    /** El usuario autenticado sale del JWT ya validado (claim sub), no de un parámetro. */
    @GetMapping("/destinatarios")
    public List<DestinatarioResponse> destinatarios(@AuthenticationPrincipal Jwt jwt) {
        return listarDestinatarios.listar(UUID.fromString(jwt.getSubject()));
    }
}
