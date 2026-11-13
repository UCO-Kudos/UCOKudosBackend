package co.edu.uco.kudos.servicios.input.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasException;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoCuenta;
import co.edu.uco.kudos.dominio.modelo.Persona;
import co.edu.uco.kudos.dominio.modelo.RegistroAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.servicios.input.dto.IniciarSesionCommand;
import co.edu.uco.kudos.servicios.input.dto.IniciarSesionResponse;
import co.edu.uco.kudos.servicios.output.port.AuditLogPort;
import co.edu.uco.kudos.servicios.output.port.PersonaRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.TokenServicePort;
import co.edu.uco.kudos.servicios.output.port.VerificadorClavePort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IniciarSesionServiceTest {

    private static final String CORREO = "autor@uco-kudos.local";
    private static final String HASH = "$2a$10$hashDePrueba";

    @Mock
    private PersonaRepositoryPort personas;
    @Mock
    private VerificadorClavePort verificadorClave;
    @Mock
    private TokenServicePort tokens;
    @Mock
    private AuditLogPort auditoria;
    @InjectMocks
    private IniciarSesionService servicio;

    private final Persona autor = persona(EstadoCuenta.ACTIVA);

    @Test
    void loginCorrectoDevuelveTokenYAuditaExito() {
        when(personas.buscarPorCorreo(CORREO)).thenReturn(Optional.of(autor));
        when(personas.buscarHashClave(autor.id())).thenReturn(Optional.of(HASH));
        when(verificadorClave.coincide("Kudos2026*", HASH)).thenReturn(true);
        when(tokens.emitir(autor.id(), Rol.ESTUDIANTE)).thenReturn("jwt-firmado");

        IniciarSesionResponse respuesta = servicio.iniciarSesion(new IniciarSesionCommand(CORREO, "Kudos2026*", "trace-1"));

        assertEquals("jwt-firmado", respuesta.token());
        assertEquals(autor.id(), respuesta.personaId());
        assertEquals("ESTUDIANTE", respuesta.rol());
        RegistroAuditoria registro = auditoriaRegistrada();
        assertEquals(AccionAuditoria.LOGIN_EXITOSO, registro.accion());
        assertEquals(ResultadoAuditoria.EXITO, registro.resultado());
        assertEquals("trace-1", registro.traceId());
    }

    @Test
    void claveIncorrectaRechazaYAuditaFallo() {
        when(personas.buscarPorCorreo(CORREO)).thenReturn(Optional.of(autor));
        when(personas.buscarHashClave(autor.id())).thenReturn(Optional.of(HASH));
        when(verificadorClave.coincide("otra", HASH)).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class,
                () -> servicio.iniciarSesion(new IniciarSesionCommand(CORREO, "otra", "trace-2")));

        verify(tokens, never()).emitir(any(), any());
        RegistroAuditoria registro = auditoriaRegistrada();
        assertEquals(AccionAuditoria.LOGIN_FALLIDO, registro.accion());
        assertEquals(ResultadoAuditoria.FALLO, registro.resultado());
        assertEquals("trace-2", registro.traceId());
    }

    @Test
    void correoInexistenteRechazaConElMismoMensaje() {
        when(personas.buscarPorCorreo("nadie@uco.edu.co")).thenReturn(Optional.empty());

        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> servicio.iniciarSesion(new IniciarSesionCommand("nadie@uco.edu.co", "x", "trace-3")));

        assertEquals("Credenciales inválidas", error.getMessage());
        assertEquals(AccionAuditoria.LOGIN_FALLIDO, auditoriaRegistrada().accion());
    }

    @Test
    void cuentaNoActivaNoPuedeIniciarSesion() {
        Persona suspendida = persona(EstadoCuenta.SUSPENDIDA);
        when(personas.buscarPorCorreo(CORREO)).thenReturn(Optional.of(suspendida));

        assertThrows(CredencialesInvalidasException.class,
                () -> servicio.iniciarSesion(new IniciarSesionCommand(CORREO, "Kudos2026*", "trace-4")));

        verify(verificadorClave, never()).coincide(any(), any());
        verify(tokens, never()).emitir(any(), any());
    }

    private RegistroAuditoria auditoriaRegistrada() {
        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(auditoria).registrar(captor.capture());
        return captor.getValue();
    }

    private static Persona persona(EstadoCuenta estado) {
        return new Persona(UUID.randomUUID(), "Estudiante", "Autor", CORREO, "+570000000002", estado, Rol.ESTUDIANTE);
    }
}
