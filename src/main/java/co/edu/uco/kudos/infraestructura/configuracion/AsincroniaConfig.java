package co.edu.uco.kudos.infraestructura.configuracion;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @EnableAsync para el listener de notificaciones y @EnableScheduling para el latido del SSE.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsincroniaConfig {

    public static final String EJECUTOR_NOTIFICACIONES = "ejecutorNotificaciones";

    /**
     * Mampara (bulkhead): las notificaciones usan sus propios hilos, así un Brevo o Twilio lento
     * no consume los hilos que atienden las peticiones HTTP.
     */
    @Bean(name = EJECUTOR_NOTIFICACIONES)
    public ThreadPoolTaskExecutor ejecutorNotificaciones() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(100);
        ejecutor.setThreadNamePrefix("notificaciones-");
        // Si la cola se llena, notifica el hilo que publicó: más lento, pero no se pierde la notificación.
        ejecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Apagado ordenado: se esperan las notificaciones en curso.
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(30);
        return ejecutor;
    }
}
