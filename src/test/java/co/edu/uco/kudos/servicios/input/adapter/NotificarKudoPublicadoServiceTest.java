package co.edu.uco.kudos.servicios.input.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.dominio.excepcion.EnvioFallidoException;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.CanalNotificacion;
import co.edu.uco.kudos.dominio.modelo.EstadoCuenta;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.Persona;
import co.edu.uco.kudos.dominio.modelo.RegistroAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.servicios.output.port.AuditLogPort;
import co.edu.uco.kudos.servicios.output.port.EmailSenderPort;
import co.edu.uco.kudos.servicios.output.port.MetricasNotificacionPort;
import co.edu.uco.kudos.servicios.output.port.NotificacionRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.PersonaRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.PushNotifierPort;
import co.edu.uco.kudos.servicios.output.port.WhatsAppSenderPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificarKudoPublicadoServiceTest {

    @Mock
    private PersonaRepositoryPort personas;
    @Mock
    private EmailSenderPort correo;
    @Mock
    private WhatsAppSenderPort whatsApp;
    @Mock
    private PushNotifierPort push;
    @Mock
    private NotificacionRepositoryPort notificaciones;
    @Mock
    private AuditLogPort auditoria;
    @Mock
    private MetricasNotificacionPort metricas;
    @InjectMocks
    private NotificarKudoPublicadoService servicio;

    @Captor
    private ArgumentCaptor<Notificacion> notificacionesGuardadas;
    @Captor
    private ArgumentCaptor<RegistroAuditoria> registrosAuditoria;

    private final Persona autor = persona("Ana");
    private final Persona destinatario = persona("Luis");
    private final KudoPublicado evento =
            new KudoPublicado(UUID.randomUUID(), autor.id(), destinatario.id(), "trace-1");

    @BeforeEach
    void preparar() {
        when(personas.buscarPorId(autor.id())).thenReturn(Optional.of(autor));
        when(personas.buscarPorId(destinatario.id())).thenReturn(Optional.of(destinatario));
    }

    @Test
    void siFallaElCorreoWhatsAppYPushSiguenYTodoQuedaAuditadoConElMismoTraceId() {
        when(correo.enviar(anyString(), anyString(), anyString()))
                .thenThrow(new EnvioFallidoException("Brevo respondió 503", 3));
        when(whatsApp.enviar(anyString(), anyString())).thenReturn(1);
        when(push.enviar(destinatario.id(), evento.kudoId(), mensajeEsperado())).thenReturn(true);

        servicio.notificar(evento);

        List<Notificacion> guardadas = notificacionesGuardadas();
        assertEquals(EstadoNotificacion.FALLIDA, guardadas.get(0).estado());
        assertEquals(3, guardadas.get(0).intentos());
        assertEquals("Brevo respondió 503", guardadas.get(0).ultimoError());
        assertEquals(EstadoNotificacion.ENVIADA, guardadas.get(1).estado());
        assertEquals(EstadoNotificacion.ENVIADA, guardadas.get(2).estado());
        assertNull(guardadas.get(2).ultimoError());

        List<RegistroAuditoria> registros = registrosAuditoria();
        assertEquals(List.of(AccionAuditoria.NOTIFICACION_CORREO_FALLIDA,
                        AccionAuditoria.NOTIFICACION_WHATSAPP_ENVIADA,
                        AccionAuditoria.NOTIFICACION_PUSH_ENVIADA),
                registros.stream().map(RegistroAuditoria::accion).toList());
        registros.forEach(registro -> assertEquals("trace-1", registro.traceId()));
        verify(metricas).contarNotificacion(CanalNotificacion.CORREO, EstadoNotificacion.FALLIDA);
    }

    @Test
    void canalSinCredencialesQuedaFallidoConCeroIntentosYNoDetieneLosDemas() {
        when(correo.enviar(anyString(), anyString(), anyString())).thenReturn(1);
        when(whatsApp.enviar(anyString(), anyString()))
                .thenThrow(new EnvioFallidoException("WhatsApp no configurado: faltan credenciales de Twilio", 0));
        when(push.enviar(any(), any(), anyString())).thenReturn(false);

        servicio.notificar(evento);

        List<Notificacion> guardadas = notificacionesGuardadas();
        assertEquals(EstadoNotificacion.ENVIADA, guardadas.get(0).estado());
        assertEquals(EstadoNotificacion.FALLIDA, guardadas.get(1).estado());
        assertEquals(0, guardadas.get(1).intentos());
        assertEquals(EstadoNotificacion.OMITIDA, guardadas.get(2).estado());

        assertEquals(List.of(AccionAuditoria.NOTIFICACION_CORREO_ENVIADA,
                        AccionAuditoria.NOTIFICACION_WHATSAPP_FALLIDA,
                        AccionAuditoria.NOTIFICACION_PUSH_OMITIDA_DESCONECTADO),
                registrosAuditoria().stream().map(RegistroAuditoria::accion).toList());
    }

    @Test
    void unErrorInesperadoEnUnCanalTampocoDetieneLosDemas() {
        when(correo.enviar(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("bug"));
        when(whatsApp.enviar(anyString(), anyString())).thenReturn(2);
        when(push.enviar(any(), any(), anyString())).thenReturn(true);

        servicio.notificar(evento);

        List<Notificacion> guardadas = notificacionesGuardadas();
        assertEquals(EstadoNotificacion.FALLIDA, guardadas.get(0).estado());
        assertEquals(2, guardadas.get(1).intentos());
        assertEquals(EstadoNotificacion.ENVIADA, guardadas.get(2).estado());
    }

    @Test
    void siFallaGuardarElResultadoDeUnCanalLosSiguientesSeProcesan() {
        when(correo.enviar(anyString(), anyString(), anyString())).thenReturn(1);
        when(whatsApp.enviar(anyString(), anyString())).thenReturn(1);
        when(push.enviar(any(), any(), anyString())).thenReturn(true);
        doThrow(new RuntimeException("BD caída")).doNothing().doNothing().when(notificaciones).guardar(any());

        servicio.notificar(evento);

        verify(whatsApp).enviar(destinatario.celular(), mensajeEsperado());
        verify(push).enviar(destinatario.id(), evento.kudoId(), mensajeEsperado());
        verify(auditoria, times(2)).registrar(any());
    }

    @Test
    void elMensajeNoIncluyeElTextoDelKudoYVaAlCorreoDelDestinatario() {
        when(correo.enviar(anyString(), anyString(), anyString())).thenReturn(1);
        when(whatsApp.enviar(anyString(), anyString())).thenReturn(1);
        doNothing().when(notificaciones).guardar(any());

        servicio.notificar(evento);

        verify(correo).enviar(destinatario.correo(), NotificarKudoPublicadoService.ASUNTO, mensajeEsperado());
    }

    private String mensajeEsperado() {
        return "Hola Luis, Ana Apellido te envió un Kudo en UCO Kudos. Entra a la aplicación para verlo.";
    }

    private List<Notificacion> notificacionesGuardadas() {
        verify(notificaciones, times(3)).guardar(notificacionesGuardadas.capture());
        List<Notificacion> guardadas = notificacionesGuardadas.getAllValues();
        assertEquals(List.of(CanalNotificacion.CORREO, CanalNotificacion.WHATSAPP, CanalNotificacion.PUSH),
                guardadas.stream().map(Notificacion::canal).toList());
        return guardadas;
    }

    private List<RegistroAuditoria> registrosAuditoria() {
        verify(auditoria, times(3)).registrar(registrosAuditoria.capture());
        return registrosAuditoria.getAllValues();
    }

    private static Persona persona(String nombres) {
        return new Persona(UUID.randomUUID(), nombres, "Apellido", nombres.toLowerCase() + "@uco.edu.co",
                "+57300000000" + nombres.length(), EstadoCuenta.ACTIVA, Rol.ESTUDIANTE);
    }
}
