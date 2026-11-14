package co.edu.uco.kudos.servicios.output.port;

import co.edu.uco.kudos.dominio.modelo.CanalNotificacion;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;

/**
 * Cuenta las notificaciones por canal y resultado. Es un puerto para que el caso de uso
 * no dependa de Micrometer (AR-05).
 */
public interface MetricasNotificacionPort {

    void contarNotificacion(CanalNotificacion canal, EstadoNotificacion estado);
}
