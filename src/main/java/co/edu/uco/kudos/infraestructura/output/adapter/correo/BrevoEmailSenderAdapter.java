package co.edu.uco.kudos.infraestructura.output.adapter.correo;

import co.edu.uco.kudos.dominio.excepcion.EnvioFallidoException;
import co.edu.uco.kudos.infraestructura.output.adapter.resiliencia.LlamadaResiliente;
import co.edu.uco.kudos.servicios.output.port.EmailSenderPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Envía el correo con la API HTTPS de Brevo (POST /v3/smtp/email, cabecera api-key), sin SDK.
 * Si faltan BREVO_API_KEY o MAIL_FROM la aplicación arranca igual: cada envío queda FALLIDA
 * con 0 intentos y no se llama a Brevo.
 */
@Component
public class BrevoEmailSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailSenderAdapter.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String remitente;
    private final Retry reintento;
    private final CircuitBreaker circuito;

    public BrevoEmailSenderAdapter(@Value("${kudos.brevo.url}") String urlBrevo,
                                   @Value("${kudos.brevo.api-key}") String apiKey,
                                   @Value("${kudos.brevo.remitente}") String remitente,
                                   RetryRegistry reintentos, CircuitBreakerRegistry circuitos) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder().baseUrl(urlBrevo).requestFactory(fabrica).build();
        this.apiKey = apiKey;
        this.remitente = remitente;
        this.reintento = reintentos.retry("correo");
        this.circuito = circuitos.circuitBreaker("correo");
        if (!configurado()) {
            log.warn("Canal de correo sin configurar (faltan BREVO_API_KEY o MAIL_FROM): los envíos quedarán FALLIDA");
        }
    }

    @Override
    public int enviar(String para, String asunto, String cuerpo) {
        if (!configurado()) {
            throw new EnvioFallidoException("Correo no configurado: faltan BREVO_API_KEY o MAIL_FROM", 0);
        }
        Map<String, Object> correo = Map.of(
                "sender", Map.of("name", "UCO Kudos", "email", remitente),
                "to", List.of(Map.of("email", para)),
                "subject", asunto,
                "textContent", cuerpo);
        return LlamadaResiliente.ejecutar("Brevo", reintento, circuito, () -> restClient.post()
                .uri("/v3/smtp/email")
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(correo)
                .retrieve()
                .toBodilessEntity());
    }

    private boolean configurado() {
        return !apiKey.isBlank() && !remitente.isBlank();
    }
}
