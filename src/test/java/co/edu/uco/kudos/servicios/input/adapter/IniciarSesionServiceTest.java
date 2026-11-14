package co.edu.uco.kudos.servicios.input.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasException;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.DominioInstitucional;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IniciarSesionServiceTest {

    private static final String CORREO = "autor@uco.net.co";
    private static final String HASH = "$2a$10$hashDePrueba";

    @Mock
    private PersonaRepositoryPort personas;
    @Mock
    private VerificadorClavePort verificadorClave;
    @Mock
    private TokenServicePort tokens;
    @Mock
    private AuditLogPort auditoria;
    private IniciarSesionService servicio;

    private final Persona autor = persona(EstadoCuenta.ACTIVA);

    @BeforeEach
    void preparar() {
        servicio = new IniciarSesionService(personas, verificadorClave, tokens, auditoria,
                new DominioInstitucional("uco.net.co"));
    }

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
        when(personas.buscarPorCorreo("nadie@uco.net.co")).thenReturn(Optional.empty());

        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> servicio.iniciarSesion(new IniciarSesionCommand("nadie@uco.net.co", "x", "trace-3")));

        assertEquals("Credenciales inválidas", error.getMessage());
        assertEquals(AccionAuditoria.LOGIN_FALLIDO, auditoriaRegistrada().accion());
    }

    @Test
    void correoDeOtroDominioRechazaSinConsultarLaBdYQuedaAuditado() {
        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> servicio.iniciarSesion(new IniciarSesionCommand("autor@gmail.com", "Kudos2026*", "trace-5")));

        assertEquals("Credenciales inválidas", error.getMessage());
        verifyNoInteractions(personas, verificadorClave, tokens);
        RegistroAuditoria registro = auditoriaRegistrada();
        assertEquals(AccionAuditoria.LOGIN_FALLIDO, registro.accion());
        assertEquals(ResultadoAuditoria.FALLO, registro.resultado());
        assertEquals("correo no institucional", registro.detalle().get("motivo"));
        assertEquals("trace-5", registro.traceId());
    }

    @Test
    void personaGuardadaConCorreoDeOtroDominioTampocoEntra() {
        Persona externa = new Persona(UUID.randomUUID(), "Externo", "Persona", "externo@otro.edu.co",
                "+570000000009", EstadoCuenta.ACTIVA, Rol.ESTUDIANTE);
        // Caso artificial (el repositorio devuelve otra fila): prueba la segunda barrera del login.
        when(personas.buscarPorCorreo(CORREO)).thenReturn(Optional.of(externa));

        assertThrows(CredencialesInvalidasException.class,
                () -> servicio.iniciarSesion(new IniciarSesionCommand(CORREO, "Kudos2026*", "trace-6")));

        verify(verificadorClave, never()).coincide(any(), any());
        assertEquals("correo no institucional", auditoriaRegistrada().detalle().get("motivo"));
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
