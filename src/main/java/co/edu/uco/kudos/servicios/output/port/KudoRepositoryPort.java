package co.edu.uco.kudos.servicios.output.port;

import co.edu.uco.kudos.dominio.modelo.Adjunto;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.dominio.modelo.KudoDetalle;
import java.util.List;
import java.util.UUID;

public interface KudoRepositoryPort {

    /** Guarda el Kudo y el metadato de sus adjuntos (los archivos ya están en el almacenamiento). */
    void guardar(Kudo kudo, List<Adjunto> adjuntos);

    /** Kudos activos que la persona envió o recibió, del más reciente al más antiguo (máximo 50). */
    List<KudoDetalle> listarDePersona(UUID personaId);
}
