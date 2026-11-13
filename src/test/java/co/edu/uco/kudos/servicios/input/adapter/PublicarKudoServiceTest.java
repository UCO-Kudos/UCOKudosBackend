package co.edu.uco.kudos.servicios.input.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.dominio.excepcion.AdjuntoInvalidoException;
import co.edu.uco.kudos.dominio.excepcion.ReglaDeNegocioException;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Adjunto;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.dominio.modelo.EstadoCuenta;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.dominio.modelo.Persona;
import co.edu.uco.kudos.dominio.modelo.RegistroAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.servicios.input.dto.ArchivoAdjunto;
import co.edu.uco.kudos.servicios.input.dto.PublicarKudoCommand;
import co.edu.uco.kudos.servicios.input.dto.PublicarKudoResponse;
import co.edu.uco.kudos.servicios.output.port.AuditLogPort;
import co.edu.uco.kudos.servicios.output.port.CategoriaRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.DomainEventPublisherPort;
import co.edu.uco.kudos.servicios.output.port.FileStoragePort;
import co.edu.uco.kudos.servicios.output.port.KudoRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.PersonaRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicarKudoServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};

    @Mock
    private PersonaRepositoryPort personas;
    @Mock
    private CategoriaRepositoryPort categorias;
    @Mock
    private FileStoragePort almacenamiento;
    @Mock
    private KudoRepositoryPort kudos;
    @Mock
    private AuditLogPort auditoria;
    @Mock
    private DomainEventPublisherPort eventos;
    @InjectMocks
    private PublicarKudoService servicio;

    @Captor
    private ArgumentCaptor<List<Adjunto>> adjuntosGuardados;
    @Captor
    private ArgumentCaptor<RegistroAuditoria> registroAuditoria;

    private final Persona autor = persona();
    private final Persona destinatario = persona();
    private final Categoria categoria = new Categoria(UUID.randomUUID(), "Liderazgo", true);

    @Test
    void publicaKudoConAdjuntoAuditaYPublicaEvento() {
        prepararActores();
        when(almacenamiento.guardar(any(UUID.class), eq("image/png"), eq(PNG))).thenReturn("kudos/k/archivo");

        PublicarKudoResponse respuesta = servicio.publicar(
                comando(destinatario.id(), List.of(new ArchivoAdjunto("foto.png", "image/png", PNG))));

        assertEquals(1, respuesta.adjuntos());
        assertEquals("trace-1", respuesta.traceId());

        verify(kudos).guardar(any(Kudo.class), adjuntosGuardados.capture());
        Adjunto adjunto = adjuntosGuardados.getValue().getFirst();
        assertEquals("kudos/k/archivo", adjunto.claveAlmacenamiento());
        assertEquals(PublicarKudoService.sha256(PNG), adjunto.sha256());
        assertEquals(PNG.length, adjunto.tamanoBytes());

        verify(auditoria).registrar(registroAuditoria.capture());
        assertEquals(AccionAuditoria.KUDO_PUBLICADO, registroAuditoria.getValue().accion());
        assertEquals("trace-1", registroAuditoria.getValue().traceId());

        verify(eventos).publicar(new KudoPublicado(respuesta.id(), autor.id(), destinatario.id(), "trace-1"));
    }

    @Test
    void destinatarioInexistenteNoGuardaNada() {
        when(personas.buscarPorId(autor.id())).thenReturn(Optional.of(autor));
        when(personas.buscarPorId(destinatario.id())).thenReturn(Optional.empty());

        assertThrows(ReglaDeNegocioException.class, () -> servicio.publicar(comando(destinatario.id(), List.of())));

        verifyNoInteractions(almacenamiento, kudos, auditoria, eventos);
    }

    @Test
    void autorIgualAlDestinatarioNoGuardaNada() {
        when(personas.buscarPorId(autor.id())).thenReturn(Optional.of(autor));
        when(categorias.buscarPorId(categoria.id())).thenReturn(Optional.of(categoria));

        assertThrows(ReglaDeNegocioException.class, () -> servicio.publicar(comando(autor.id(), List.of())));

        verifyNoInteractions(almacenamiento, kudos, auditoria, eventos);
    }

    @Test
    void adjuntoDeTipoNoPermitidoNoSubeNada() {
        prepararActores();
        byte[] ejecutable = "MZ ejecutable disfrazado".getBytes(StandardCharsets.US_ASCII);
        List<ArchivoAdjunto> archivos = List.of(
                new ArchivoAdjunto("foto.png", "image/png", PNG),
                new ArchivoAdjunto("foto2.png", "image/png", ejecutable));

        assertThrows(AdjuntoInvalidoException.class, () -> servicio.publicar(comando(destinatario.id(), archivos)));

        verifyNoInteractions(almacenamiento, kudos, auditoria, eventos);
    }

    @Test
    void adjuntoMayorA5MbSeRechaza() {
        prepararActores();
        byte[] grande = Arrays.copyOf(PNG, (int) PublicarKudoService.TAMANO_MAXIMO_BYTES + 1);

        assertThrows(AdjuntoInvalidoException.class, () -> servicio.publicar(
                comando(destinatario.id(), List.of(new ArchivoAdjunto("grande.png", "image/png", grande)))));

        verifyNoInteractions(almacenamiento, kudos, auditoria, eventos);
    }

    @Test
    void elNombreOriginalSeSanitiza() {
        assertEquals("clave.png", PublicarKudoService.sanitizarNombre("../../etc/clave.png"));
        assertEquals("_script_.pdf", PublicarKudoService.sanitizarNombre("<script>.pdf"));
        assertEquals("adjunto", PublicarKudoService.sanitizarNombre(null));
    }

    private void prepararActores() {
        when(personas.buscarPorId(autor.id())).thenReturn(Optional.of(autor));
        when(personas.buscarPorId(destinatario.id())).thenReturn(Optional.of(destinatario));
        when(categorias.buscarPorId(categoria.id())).thenReturn(Optional.of(categoria));
    }

    private PublicarKudoCommand comando(UUID destinatarioId, List<ArchivoAdjunto> adjuntos) {
        return new PublicarKudoCommand(autor.id(), destinatarioId, categoria.id(), "Gracias por tu ayuda",
                adjuntos, "trace-1");
    }

    private static Persona persona() {
        return new Persona(UUID.randomUUID(), "Nombre", "Apellido", "correo@uco.edu.co", "+570000000000",
                EstadoCuenta.ACTIVA, Rol.ESTUDIANTE);
    }
}
