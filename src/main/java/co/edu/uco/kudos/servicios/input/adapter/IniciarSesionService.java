package co.edu.uco.kudos.servicios.input.adapter;

import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasException;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.DominioInstitucional;
import co.edu.uco.kudos.dominio.modelo.Persona;
import co.edu.uco.kudos.dominio.modelo.RegistroAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.servicios.input.dto.IniciarSesionCommand;
import co.edu.uco.kudos.servicios.input.dto.IniciarSesionResponse;
import co.edu.uco.kudos.servicios.input.port.IniciarSesionUseCase;
import co.edu.uco.kudos.servicios.output.port.AuditLogPort;
import co.edu.uco.kudos.servicios.output.port.PersonaRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.TokenServicePort;
import co.edu.uco.kudos.servicios.output.port.VerificadorClavePort;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Sin @Transactional a propósito: si el login falla se lanza una excepción, y dentro de una
 * transacción eso desharía también el registro LOGIN_FALLIDO de la auditoría.
 */
@Service
public class IniciarSesionService implements IniciarSesionUseCase {

    private static final String RECURSO = "PERSONA";

    private final PersonaRepositoryPort personas;
    private final VerificadorClavePort verificadorClave;
    private final TokenServicePort tokens;
    private final AuditLogPort auditoria;
    private final DominioInstitucional dominioInstitucional;

    public IniciarSesionService(PersonaRepositoryPort personas, VerificadorClavePort verificadorClave,
                                TokenServicePort tokens, AuditLogPort auditoria,
                                DominioInstitucional dominioInstitucional) {
        this.personas = personas;
        this.verificadorClave = verificadorClave;
        this.tokens = tokens;
        this.auditoria = auditoria;
        this.dominioInstitucional = dominioInstitucional;
    }

    @Override
    public IniciarSesionResponse iniciarSesion(IniciarSesionCommand comando) {
        String traceId = comando.traceId();
        String correo = comando.correo().strip();
        // Un correo de otro dominio no puede ser de nadie: se rechaza sin consultar la BD.
        if (!dominioInstitucional.admite(correo)) {
            throw registrarFallo(null, "correo no institucional", traceId);
        }
        Persona persona = personas.buscarPorCorreo(correo)
                .orElseThrow(() -> registrarFallo(null, "correo no registrado", traceId));
        // Defensa en profundidad: una fila con correo de otro dominio tampoco entra.
        if (!dominioInstitucional.admite(persona)) {
            throw registrarFallo(persona.id(), "correo no institucional", traceId);
        }
        if (!persona.estaActiva()) {
            throw registrarFallo(persona.id(), "cuenta no activa", traceId);
        }
        String hash = personas.buscarHashClave(persona.id())
                .orElseThrow(() -> registrarFallo(persona.id(), "sin clave", traceId));
        if (!verificadorClave.coincide(comando.clave(), hash)) {
            throw registrarFallo(persona.id(), "clave incorrecta", traceId);
        }

        String token = tokens.emitir(persona.id(), persona.rol());
        auditoria.registrar(RegistroAuditoria.nuevo(persona.id(), AccionAuditoria.LOGIN_EXITOSO, RECURSO,
                persona.id(), ResultadoAuditoria.EXITO, traceId, Map.of()));
        return new IniciarSesionResponse(token, persona.id(), persona.nombreCompleto(), persona.rol().name());
    }

    // El motivo real queda solo en la auditoría; al cliente siempre le llega "Credenciales inválidas".
    private CredencialesInvalidasException registrarFallo(UUID personaId, String motivo, String traceId) {
        auditoria.registrar(RegistroAuditoria.nuevo(personaId, AccionAuditoria.LOGIN_FALLIDO, RECURSO,
                personaId, ResultadoAuditoria.FALLO, traceId, Map.of("motivo", motivo)));
        return new CredencialesInvalidasException();
    }
}
