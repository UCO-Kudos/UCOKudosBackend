package co.edu.uco.kudos.servicios.input.port;

import co.edu.uco.kudos.servicios.input.dto.DestinatarioResponse;
import java.util.List;
import java.util.UUID;

public interface ListarDestinatariosUseCase {

    /** Personas activas a quienes se puede enviar un Kudo, sin incluir al usuario autenticado. */
    List<DestinatarioResponse> listar(UUID usuarioAutenticadoId);
}
