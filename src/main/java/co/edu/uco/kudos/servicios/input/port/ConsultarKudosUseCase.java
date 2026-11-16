package co.edu.uco.kudos.servicios.input.port;

import co.edu.uco.kudos.servicios.input.dto.KudoResumenResponse;
import java.util.List;
import java.util.UUID;

public interface ConsultarKudosUseCase {

    /** Kudos que el usuario envió o recibió, del más reciente al más antiguo (máximo 50). */
    List<KudoResumenResponse> listar(UUID usuarioId);
}
