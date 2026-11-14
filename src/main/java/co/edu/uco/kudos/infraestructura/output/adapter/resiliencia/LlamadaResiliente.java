package co.edu.uco.kudos.infraestructura.output.adapter.resiliencia;

import co.edu.uco.kudos.dominio.excepcion.EnvioFallidoException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Envuelve una llamada a un proveedor con Resilience4j: Retry(CircuitBreaker(llamada)).
 * Cada intento pasa por el circuit breaker; si el circuito está abierto no se llama al proveedor.
 * Se usa la API de Resilience4j directamente (no las anotaciones) porque así se puede contar
 * cada intento para la tabla notificacion, y porque las anotaciones necesitan AspectJ (AOP).
 * Los reintentos, esperas y umbrales se configuran en application.yml (resilience4j.*).
 */
public final class LlamadaResiliente {

    private LlamadaResiliente() {
    }

    /** @return cuántos intentos se hicieron hasta que el proveedor aceptó la llamada. */
    public static int ejecutar(String proveedor, Retry reintento, CircuitBreaker circuito, Runnable llamada) {
        AtomicInteger intentos = new AtomicInteger();
        Runnable conCircuito = CircuitBreaker.decorateRunnable(circuito, () -> {
            intentos.incrementAndGet();
            llamada.run();
        });
        try {
            Retry.decorateRunnable(reintento, conCircuito).run();
            return intentos.get();
        } catch (RuntimeException e) {
            throw new EnvioFallidoException(describir(proveedor, e), intentos.get(), e);
        }
    }

    // Solo el código HTTP o el tipo de falla: el cuerpo de la respuesta puede traer datos personales.
    static String describir(String proveedor, RuntimeException error) {
        if (error instanceof CallNotPermittedException) {
            return "Circuito abierto: " + proveedor + " falló varias veces seguidas; no se intentó";
        }
        if (error instanceof HttpStatusCodeException http) {
            return proveedor + " respondió " + http.getStatusCode().value();
        }
        if (error instanceof ResourceAccessException) {
            return proveedor + " no respondió (red o tiempo de espera)";
        }
        return "Error al llamar a " + proveedor + ": " + error.getClass().getSimpleName();
    }
}
