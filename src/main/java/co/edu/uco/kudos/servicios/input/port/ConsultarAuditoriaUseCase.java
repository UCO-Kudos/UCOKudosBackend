package co.edu.uco.kudos.servicios.input.port;

import co.edu.uco.kudos.servicios.input.dto.RegistroAuditoriaResponse;
import java.util.List;

public interface ConsultarAuditoriaUseCase {

    List<RegistroAuditoriaResponse> consultarPorTraceId(String traceId);
}
