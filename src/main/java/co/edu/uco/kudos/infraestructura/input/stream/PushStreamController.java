package co.edu.uco.kudos.infraestructura.input.stream;

import co.edu.uco.kudos.infraestructura.output.adapter.push.SsePushNotifierAdapter;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Abre el stream SSE del usuario autenticado. El token va en la cabecera Authorization
 * (el cliente usa fetch), nunca en la URL. Habla directo con el adaptador SSE porque abrir
 * una conexión es un detalle de transporte sin reglas de negocio, y SseEmitter es un tipo de
 * Spring que no puede entrar en un puerto de servicios (AR-03/AR-05).
 */
@RestController
public class PushStreamController {

    private final SsePushNotifierAdapter push;

    public PushStreamController(SsePushNotifierAdapter push) {
        this.push = push;
    }

    @GetMapping(path = "/api/notificaciones/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter conectar(@AuthenticationPrincipal Jwt jwt) {
        return push.conectar(UUID.fromString(jwt.getSubject()));
    }
}
