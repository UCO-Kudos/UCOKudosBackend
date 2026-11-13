package co.edu.uco.kudos.servicios.input.adapter;

import co.edu.uco.kudos.servicios.input.dto.DestinatarioResponse;
import co.edu.uco.kudos.servicios.input.port.ListarDestinatariosUseCase;
import co.edu.uco.kudos.servicios.output.port.PersonaRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Solo id y nombre: el correo y el celular de los demás no salen de la API (minimización de datos).
 */
@Service
public class ListarDestinatariosService implements ListarDestinatariosUseCase {

    private final PersonaRepositoryPort personas;

    public ListarDestinatariosService(PersonaRepositoryPort personas) {
        this.personas = personas;
    }

    @Override
    public List<DestinatarioResponse> listar(UUID usuarioAutenticadoId) {
        return personas.listarActivasExcepto(usuarioAutenticadoId).stream()
                .map(persona -> new DestinatarioResponse(persona.id(), persona.nombreCompleto()))
                .toList();
    }
}
