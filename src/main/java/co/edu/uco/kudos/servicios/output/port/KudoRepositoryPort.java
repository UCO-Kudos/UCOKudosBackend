package co.edu.uco.kudos.servicios.output.port;

import co.edu.uco.kudos.dominio.modelo.Adjunto;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import java.util.List;

public interface KudoRepositoryPort {

    /** Guarda el Kudo y el metadato de sus adjuntos (los archivos ya están en el almacenamiento). */
    void guardar(Kudo kudo, List<Adjunto> adjuntos);
}
