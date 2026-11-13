package co.edu.uco.kudos.servicios.output.port;

import java.util.UUID;

public interface FileStoragePort {

    /**
     * Guarda el archivo en el bucket privado y devuelve la clave generada por el servidor
     * (kudos/{kudoId}/{uuid}); el nombre original nunca se usa como ruta.
     */
    String guardar(UUID kudoId, String tipoMime, byte[] contenido);
}
