package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.AdjuntoInvalidoException;
import java.util.Arrays;

/**
 * Lista blanca de tipos de adjunto. El tipo se detecta por los primeros bytes del archivo
 * ("firma"), no por la extensión ni por el Content-Type que manda el cliente, que se pueden falsificar.
 */
public enum TipoAdjunto {
    PDF("application/pdf", new byte[] {'%', 'P', 'D', 'F', '-'}),
    PNG("image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}),
    JPG("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    private final String tipoMime;
    private final byte[] firma;

    TipoAdjunto(String tipoMime, byte[] firma) {
        this.tipoMime = tipoMime;
        this.firma = firma;
    }

    public static TipoAdjunto detectar(byte[] contenido) {
        for (TipoAdjunto tipo : values()) {
            if (tipo.coincideCon(contenido)) {
                return tipo;
            }
        }
        throw new AdjuntoInvalidoException("Solo se permiten adjuntos PDF, PNG o JPG");
    }

    public String tipoMime() {
        return tipoMime;
    }

    private boolean coincideCon(byte[] contenido) {
        return contenido.length >= firma.length
                && Arrays.equals(contenido, 0, firma.length, firma, 0, firma.length);
    }
}
