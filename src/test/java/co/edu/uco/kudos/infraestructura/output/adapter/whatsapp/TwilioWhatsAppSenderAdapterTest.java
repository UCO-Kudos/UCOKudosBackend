package co.edu.uco.kudos.infraestructura.output.adapter.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uco.kudos.dominio.excepcion.EnvioFallidoException;
import co.edu.uco.kudos.infraestructura.output.adapter.ServidorHttpFalso;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TwilioWhatsAppSenderAdapterTest {

    @Test
    void enviaFormularioConAutenticacionBasicaAlEndpointDeLaCuenta() {
        try (ServidorHttpFalso twilio = new ServidorHttpFalso(201)) {
            int intentos = adaptador(twilio.url(), "AC123", "token-prueba", "+14155238886")
                    .enviar("+573001234567", "Hola Luis");

            assertEquals(1, intentos);
            ServidorHttpFalso.Peticion peticion = twilio.peticiones().getFirst();
            assertEquals("/2010-04-01/Accounts/AC123/Messages.json", peticion.ruta());
            String basica = Base64.getEncoder().encodeToString("AC123:token-prueba".getBytes(StandardCharsets.UTF_8));
            assertEquals("Basic " + basica, peticion.autorizacion());
            assertTrue(peticion.contentType().startsWith("application/x-www-form-urlencoded"));
            assertTrue(peticion.cuerpo().contains("From=whatsapp%3A%2B14155238886"), peticion.cuerpo());
            assertTrue(peticion.cuerpo().contains("To=whatsapp%3A%2B573001234567"), peticion.cuerpo());
        }
    }

    @Test
    void sinCredencialesNoLlamaATwilioYFallaConCeroIntentos() {
        try (ServidorHttpFalso twilio = new ServidorHttpFalso(201)) {
            TwilioWhatsAppSenderAdapter adaptador = adaptador(twilio.url(), "", "", "");

            EnvioFallidoException error = assertThrows(EnvioFallidoException.class,
                    () -> adaptador.enviar("+573001234567", "Hola"));

            assertEquals(0, error.intentos());
            assertTrue(twilio.peticiones().isEmpty());
        }
    }

    private static TwilioWhatsAppSenderAdapter adaptador(String url, String sid, String token, String remitente) {
        return new TwilioWhatsAppSenderAdapter(url, sid, token, remitente, RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults());
    }
}
