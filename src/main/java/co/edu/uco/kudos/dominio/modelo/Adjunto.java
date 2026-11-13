package co.edu.uco.kudos.dominio.modelo;

import java.util.UUID;

/**
 * Metadato de un archivo adjunto a un Kudo. El archivo vive en el almacenamiento;
 * aquí solo se guarda dónde está (clave) y su huella (sha256) para verificar integridad.
 */
public record Adjunto(
        UUID id,
        String nombreOriginal,
        String tipoMime,
        long tamanoBytes,
        String sha256,
        String claveAlmacenamiento) {
}
