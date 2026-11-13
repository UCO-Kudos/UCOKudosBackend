package co.edu.uco.kudos.infraestructura.input.controlador;

import co.edu.uco.kudos.infraestructura.configuracion.TraceIdFilter;
import co.edu.uco.kudos.servicios.input.dto.IniciarSesionCommand;
import co.edu.uco.kudos.servicios.input.dto.IniciarSesionResponse;
import co.edu.uco.kudos.servicios.input.port.IniciarSesionUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IniciarSesionUseCase iniciarSesion;

    public AuthController(IniciarSesionUseCase iniciarSesion) {
        this.iniciarSesion = iniciarSesion;
    }

    @PostMapping("/login")
    public IniciarSesionResponse login(@Valid @RequestBody IniciarSesionRequest solicitud) {
        return iniciarSesion.iniciarSesion(
                new IniciarSesionCommand(solicitud.correo(), solicitud.clave(), TraceIdFilter.actual()));
    }
}
