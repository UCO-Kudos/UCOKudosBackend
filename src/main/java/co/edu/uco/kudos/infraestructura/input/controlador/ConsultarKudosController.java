package co.edu.uco.kudos.infraestructura.input.controlador;

import co.edu.uco.kudos.servicios.input.dto.KudoResumenResponse;
import co.edu.uco.kudos.servicios.input.port.ConsultarKudosUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kudos")
public class ConsultarKudosController {

    private final ConsultarKudosUseCase consultarKudos;

    public ConsultarKudosController(ConsultarKudosUseCase consultarKudos) {
        this.consultarKudos = consultarKudos;
    }

    /** El usuario sale del JWT ya validado (claim sub): cada quien ve solo sus propios Kudos. */
    @GetMapping
    public List<KudoResumenResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return consultarKudos.listar(UUID.fromString(jwt.getSubject()));
    }
}
