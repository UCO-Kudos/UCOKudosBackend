package co.edu.uco.kudos.servicios.input.dto;

/**
 * Archivo tal como llega del cliente, antes de validarlo y guardarlo.
 */
public record ArchivoAdjunto(String nombreOriginal, String tipoMime, byte[] contenido) {
}
