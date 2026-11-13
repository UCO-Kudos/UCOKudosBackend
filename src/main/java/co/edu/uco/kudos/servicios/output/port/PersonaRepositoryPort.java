package co.edu.uco.kudos.servicios.output.port;

import co.edu.uco.kudos.dominio.modelo.Persona;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonaRepositoryPort {

    Optional<Persona> buscarPorId(UUID id);

    Optional<Persona> buscarPorCorreo(String correo);

    /** El hash BCrypt se pide aparte para que no viaje dentro de Persona. */
    Optional<String> buscarHashClave(UUID personaId);

    List<Persona> listarActivasExcepto(UUID personaId);
}
