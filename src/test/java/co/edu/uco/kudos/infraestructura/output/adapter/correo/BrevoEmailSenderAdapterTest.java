package co.edu.uco.kudos.infraestructura.output.adapter.correo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uco.kudos.dominio.excepcion.EnvioFallidoException;
import co.edu.uco.kudos.infraestructura.output.adapter.ServidorHttpFalso;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class BrevoEmailSenderAdapterTest {

    // Misma política que application.yml, pero sin esperar entre intentos.
    private static final RetryRegistry REINTENTOS = RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1))
            .retryExceptions(HttpServerErrorException.class, ResourceAccessException.class)
            .build());

    @Test
    void enviaElCorreoConLaApiKeyEnLaCabeceraYJson() {
        try (ServidorHttpFalso brevo = new ServidorHttpFalso(201)) {
            int intentos = adaptador(brevo.url(), "clave-prueba", "kudos@uco.net.co")
                    .enviar("luis@uco.net.co", "Recibiste un Kudo", "Hola Luis");

            assertEquals(1, intentos);
            ServidorHttpFalso.Peticion peticion = brevo.peticiones().getFirst();
            assertEquals("POST", peticion.metodo());
            assertEquals("/v3/smtp/email", peticion.ruta());
            assertEquals("clave-prueba", peticion.apiKey());
            assertTrue(peticion.contentType().startsWith("application/json"));
            assertTrue(peticion.cuerpo().contains("\"to\":[{\"email\":\"luis@uco.net.co\"}]"), peticion.cuerpo());
            assertTrue(peticion.cuerpo().contains("\"email\":\"kudos@uco.net.co\""), peticion.cuerpo());
        }
    }

    @Test
    void reintentaLosErrores5xxHastaTresVecesYLuegoFalla() {
        try (ServidorHttpFalso brevo = new ServidorHttpFalso(503)) {
            BrevoEmailSenderAdapter adaptador = adaptador(brevo.url(), "clave-prueba", "kudos@uco.net.co");

            EnvioFallidoException error = assertThrows(EnvioFallidoException.class,
                    () -> adaptador.enviar("luis@uco.net.co", "Asunto", "Cuerpo"));

            assertEquals(3, error.intentos());
            assertEquals(3, brevo.peticiones().size());
            assertEquals("Brevo respondió 503", error.getMessage());
        }
    }

    @Test
    void siSeRecuperaEnElSegundoIntentoQuedaEnviadoConDosIntentos() {
        try (ServidorHttpFalso brevo = new ServidorHttpFalso(500, 201)) {
            int intentos = adaptador(brevo.url(), "clave-prueba", "kudos@uco.net.co")
                    .enviar("luis@uco.net.co", "Asunto", "Cuerpo");

            assertEquals(2, intentos);
        }
    }

    @Test
    void unError4xxNoSeReintenta() {
        try (ServidorHttpFalso brevo = new ServidorHttpFalso(401)) {
            BrevoEmailSenderAdapter adaptador = adaptador(brevo.url(), "clave-mala", "kudos@uco.net.co");

            EnvioFallidoException error = assertThrows(EnvioFallidoException.class,
                    () -> adaptador.enviar("luis@uco.net.co", "Asunto", "Cuerpo"));

            assertEquals(1, error.intentos());
        }
    }

    @Test
    void sinCredencialesNoLlamaABrevoYFallaConCeroIntentos() {
        try (ServidorHttpFalso brevo = new ServidorHttpFalso(201)) {
            BrevoEmailSenderAdapter adaptador = adaptador(brevo.url(), "", "");

            EnvioFallidoException error = assertThrows(EnvioFallidoException.class,
                    () -> adaptador.enviar("luis@uco.net.co", "Asunto", "Cuerpo"));

            assertEquals(0, error.intentos());
            assertTrue(brevo.peticiones().isEmpty());
        }
    }

    private static BrevoEmailSenderAdapter adaptador(String url, String apiKey, String remitente) {
        return new BrevoEmailSenderAdapter(url, apiKey, remitente, REINTENTOS, CircuitBreakerRegistry.ofDefaults());
    }
}
