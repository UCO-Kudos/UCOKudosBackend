package co.edu.uco.kudos.servicios.output.port;

import co.edu.uco.kudos.dominio.modelo.RegistroAuditoria;
import java.util.List;

/**
 * Auditoría de solo inserción: no hay métodos para modificar ni borrar.
 */
public interface AuditLogPort {

    void registrar(RegistroAuditoria registro);

    List<RegistroAuditoria> buscarPorTraceId(String traceId);
}
