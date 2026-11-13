package co.edu.uco.kudos.servicios.output.port;

import co.edu.uco.kudos.dominio.modelo.Rol;
import java.util.UUID;

public interface TokenServicePort {

    /** Emite un token de acceso de vida corta con el id de la persona y su rol. */
    String emitir(UUID personaId, Rol rol);
}
