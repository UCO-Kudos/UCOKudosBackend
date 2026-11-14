package co.edu.uco.kudos.infraestructura.output.adapter.whatsapp;

import co.edu.uco.kudos.dominio.excepcion.EnvioFallidoException;
import co.edu.uco.kudos.infraestructura.output.adapter.resiliencia.LlamadaResiliente;
import co.edu.uco.kudos.servicios.output.port.WhatsAppSenderPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Envía el WhatsApp con la API REST de Twilio (sandbox), sin SDK:
 * POST /2010-04-01/Accounts/{SID}/Messages.json, formulario From/To/Body y autenticación básica SID:token.
 * Si faltan las credenciales la aplicación arranca igual: cada envío queda FALLIDA con 0 intentos.
 */
@Component
public class TwilioWhatsAppSenderAdapter implements WhatsAppSenderPort {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppSenderAdapter.class);
    private static final String PREFIJO = "whatsapp:";

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String remitente;
    private final Retry reintento;
    private final CircuitBreaker circuito;

    public TwilioWhatsAppSenderAdapter(@Value("${kudos.twilio.url}") String urlTwilio,
                                       @Value("${kudos.twilio.account-sid}") String accountSid,
                                       @Value("${kudos.twilio.auth-token}") String authToken,
                                       @Value("${kudos.twilio.whatsapp-from}") String remitente,
                                       RetryRegistry reintentos, CircuitBreakerRegistry circuitos) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder().baseUrl(urlTwilio).requestFactory(fabrica).build();
        this.accountSid = accountSid;
        this.authToken = authToken;
        // Se acepta TWILIO_WHATSAPP_FROM con o sin el prefijo "whatsapp:".
        this.remitente = remitente.isBlank() || remitente.startsWith(PREFIJO) ? remitente : PREFIJO + remitente;
        this.reintento = reintentos.retry("whatsapp");
        this.circuito = circuitos.circuitBreaker("whatsapp");
        if (!configurado()) {
            log.warn("Canal de WhatsApp sin configurar (faltan TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN o "
                    + "TWILIO_WHATSAPP_FROM): los envíos quedarán FALLIDA");
        }
    }

    @Override
    public int enviar(String celular, String mensaje) {
        if (!configurado()) {
            throw new EnvioFallidoException("WhatsApp no configurado: faltan credenciales de Twilio", 0);
        }
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("From", remitente);
        formulario.add("To", PREFIJO + celular);
        formulario.add("Body", mensaje);
        return LlamadaResiliente.ejecutar("Twilio", reintento, circuito, () -> restClient.post()
                .uri("/2010-04-01/Accounts/{sid}/Messages.json", accountSid)
                .headers(cabeceras -> cabeceras.setBasicAuth(accountSid, authToken))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .toBodilessEntity());
    }

    private boolean configurado() {
        return !accountSid.isBlank() && !authToken.isBlank() && !remitente.isBlank();
    }
}
