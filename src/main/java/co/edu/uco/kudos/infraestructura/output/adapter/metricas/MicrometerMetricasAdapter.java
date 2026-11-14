package co.edu.uco.kudos.infraestructura.output.adapter.metricas;

import co.edu.uco.kudos.dominio.modelo.CanalNotificacion;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.servicios.output.port.MetricasNotificacionPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Contador Micrometer "notificaciones" con etiquetas canal y resultado (docs/07 §2).
 * Se consulta en /actuator/metrics/notificaciones?tag=canal:CORREO&tag=resultado:FALLIDA
 */
@Component
public class MicrometerMetricasAdapter implements MetricasNotificacionPort {

    static final String NOMBRE = "notificaciones";

    private final MeterRegistry registro;

    public MicrometerMetricasAdapter(MeterRegistry registro) {
        this.registro = registro;
    }

    @Override
    public void contarNotificacion(CanalNotificacion canal, EstadoNotificacion estado) {
        registro.counter(NOMBRE, "canal", canal.name(), "resultado", estado.name()).increment();
    }
}
