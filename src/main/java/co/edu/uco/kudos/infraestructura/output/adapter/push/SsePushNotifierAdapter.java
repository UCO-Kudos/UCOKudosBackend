package co.edu.uco.kudos.infraestructura.output.adapter.push;

import co.edu.uco.kudos.servicios.output.port.PushNotifierPort;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Push en tiempo real con Server-Sent Events. Guarda en memoria las conexiones abiertas de cada
 * persona (puede tener varias pestañas). Si el destinatario no tiene ninguna, el envío se omite:
 * no es un error. Limitación conocida: con varias instancias haría falta un bus (Redis Pub/Sub).
 */
@Component
public class SsePushNotifierAdapter implements PushNotifierPort {

    static final String EVENTO = "kudo-publicado";
    private static final long DURACION_CONEXION_MS = Duration.ofMinutes(30).toMillis();

    private final Map<UUID, Set<SseEmitter>> conexiones = new ConcurrentHashMap<>();

    /** Abre una conexión SSE para la persona; el navegador se reconecta cuando vence. */
    public SseEmitter conectar(UUID personaId) {
        SseEmitter emisor = new SseEmitter(DURACION_CONEXION_MS);
        conexiones.computeIfAbsent(personaId, id -> ConcurrentHashMap.newKeySet()).add(emisor);
        emisor.onCompletion(() -> quitar(personaId, emisor));
        emisor.onTimeout(emisor::complete);
        emisor.onError(error -> quitar(personaId, emisor));
        // Primer evento: confirma la conexión al cliente y fuerza el envío de las cabeceras.
        intentarEnviar(personaId, emisor, SseEmitter.event().name("conectado").data("ok"));
        return emisor;
    }

    @Override
    public boolean enviar(UUID destinatarioId, UUID kudoId, String mensaje) {
        boolean entregado = false;
        for (SseEmitter emisor : conexiones.getOrDefault(destinatarioId, Set.of())) {
            SseEmitter.SseEventBuilder evento = SseEmitter.event().name(EVENTO).id(kudoId.toString())
                    .data(Map.of("kudoId", kudoId, "mensaje", mensaje));
            entregado |= intentarEnviar(destinatarioId, emisor, evento);
        }
        return entregado;
    }

    /** Latido: un comentario SSE cada 25 s evita que los proxies cierren la conexión y detecta las caídas. */
    @Scheduled(fixedDelay = 25_000)
    public void latido() {
        conexiones.forEach((personaId, emisores) -> emisores.forEach(
                emisor -> intentarEnviar(personaId, emisor, SseEmitter.event().comment("latido"))));
    }

    int conexionesAbiertas(UUID personaId) {
        return conexiones.getOrDefault(personaId, Set.of()).size();
    }

    // Si el navegador ya cerró, enviar falla: se descarta esa conexión y se informa que no se entregó.
    private boolean intentarEnviar(UUID personaId, SseEmitter emisor, SseEmitter.SseEventBuilder evento) {
        try {
            emisor.send(evento);
            return true;
        } catch (IOException | IllegalStateException e) {
            quitar(personaId, emisor);
            return false;
        }
    }

    private void quitar(UUID personaId, SseEmitter emisor) {
        conexiones.computeIfPresent(personaId, (id, emisores) -> {
            emisores.remove(emisor);
            return emisores.isEmpty() ? null : emisores;
        });
    }
}
