package co.edu.uco.kudos.servicios.input.adapter;

import co.edu.uco.kudos.servicios.input.dto.RegistroAuditoriaResponse;
import co.edu.uco.kudos.servicios.input.port.ConsultarAuditoriaUseCase;
import co.edu.uco.kudos.servicios.output.port.AuditLogPort;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Reconstruye el flujo de una petición: todas las filas de auditoría con el mismo traceId,
 * en orden cronológico (el orden lo da la consulta del adaptador).
 */
@Service
public class ConsultarAuditoriaService implements ConsultarAuditoriaUseCase {

    private final AuditLogPort auditoria;

    public ConsultarAuditoriaService(AuditLogPort auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public List<RegistroAuditoriaResponse> consultarPorTraceId(String traceId) {
        return auditoria.buscarPorTraceId(traceId).stream()
                .map(registro -> new RegistroAuditoriaResponse(registro.id(), registro.ocurridoEn(),
                        registro.actorId(), registro.accion().name(), registro.recursoTipo(), registro.recursoId(),
                        registro.resultado().name(), registro.traceId(), registro.detalle()))
                .toList();
    }
}
