package co.edu.uco.kudos.infraestructura.input.controlador;

import co.edu.uco.kudos.servicios.input.dto.RegistroAuditoriaResponse;
import co.edu.uco.kudos.servicios.input.port.ConsultarAuditoriaUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Solo ADMIN: la regla está en SeguridadConfig. */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final ConsultarAuditoriaUseCase consultarAuditoria;

    public AuditoriaController(ConsultarAuditoriaUseCase consultarAuditoria) {
        this.consultarAuditoria = consultarAuditoria;
    }

    @GetMapping
    public List<RegistroAuditoriaResponse> consultar(@RequestParam @NotBlank @Size(max = 64) String traceId) {
        return consultarAuditoria.consultarPorTraceId(traceId);
    }
}
