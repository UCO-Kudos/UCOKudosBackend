package co.edu.uco.kudos.infraestructura.output.adapter.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SsePushNotifierAdapterTest {

    private final SsePushNotifierAdapter push = new SsePushNotifierAdapter();
    private final UUID destinatarioId = UUID.randomUUID();

    @Test
    void sinConexionAbiertaElPushSeOmite() {
        assertFalse(push.enviar(destinatarioId, UUID.randomUUID(), "Hola"));
    }

    @Test
    void conConexionAbiertaElPushSeEntrega() {
        push.conectar(destinatarioId);

        assertTrue(push.enviar(destinatarioId, UUID.randomUUID(), "Hola"));
        assertFalse(push.enviar(UUID.randomUUID(), UUID.randomUUID(), "Hola"), "Solo al destinatario");
    }

    @Test
    void unaConexionCerradaSeDescartaYElPushSeOmite() {
        SseEmitter emisor = push.conectar(destinatarioId);
        assertEquals(1, push.conexionesAbiertas(destinatarioId));

        emisor.complete();

        assertFalse(push.enviar(destinatarioId, UUID.randomUUID(), "Hola"));
        assertEquals(0, push.conexionesAbiertas(destinatarioId));
    }
}
