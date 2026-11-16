package co.edu.uco.kudos.servicios.input.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import co.edu.uco.kudos.dominio.modelo.KudoDetalle;
import co.edu.uco.kudos.servicios.input.dto.KudoResumenResponse;
import co.edu.uco.kudos.servicios.output.port.KudoRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarKudosServiceTest {

    @Mock
    private KudoRepositoryPort kudos;
    @InjectMocks
    private ConsultarKudosService servicio;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID otraPersonaId = UUID.randomUUID();

    @Test
    void marcaComoRecibidoElKudoDondeElUsuarioEsDestinatario() {
        KudoDetalle kudo = new KudoDetalle(UUID.randomUUID(), otraPersonaId, "Ana Gómez",
                usuarioId, "Luis Pérez", "Trabajo en equipo", "Gracias por tu ayuda", Instant.now());
        when(kudos.listarDePersona(usuarioId)).thenReturn(List.of(kudo));

        List<KudoResumenResponse> respuesta = servicio.listar(usuarioId);

        assertEquals(1, respuesta.size());
        KudoResumenResponse resumen = respuesta.get(0);
        assertEquals("RECIBIDO", resumen.direccion());
        assertEquals(kudo.id(), resumen.id());
        assertEquals("Ana Gómez", resumen.autor());
        assertEquals("Luis Pérez", resumen.destinatario());
        assertEquals("Trabajo en equipo", resumen.categoria());
        assertEquals("Gracias por tu ayuda", resumen.texto());
        assertEquals(kudo.creadoEn(), resumen.creadoEn());
    }

    @Test
    void marcaComoEnviadoElKudoDondeElUsuarioEsAutor() {
        KudoDetalle kudo = new KudoDetalle(UUID.randomUUID(), usuarioId, "Luis Pérez",
                otraPersonaId, "Ana Gómez", "Liderazgo", "Excelente exposición", Instant.now());
        when(kudos.listarDePersona(usuarioId)).thenReturn(List.of(kudo));

        List<KudoResumenResponse> respuesta = servicio.listar(usuarioId);

        assertEquals(1, respuesta.size());
        assertEquals("ENVIADO", respuesta.get(0).direccion());
    }

    @Test
    void devuelveListaVaciaSiElUsuarioNoTieneKudos() {
        when(kudos.listarDePersona(usuarioId)).thenReturn(List.of());

        assertTrue(servicio.listar(usuarioId).isEmpty());
    }
}
