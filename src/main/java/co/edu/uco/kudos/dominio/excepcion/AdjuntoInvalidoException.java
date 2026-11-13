package co.edu.uco.kudos.dominio.excepcion;

/**
 * El adjunto no es de un tipo permitido, está vacío o es demasiado grande.
 * La API la traduce a 400 (problem+json).
 */
public class AdjuntoInvalidoException extends RuntimeException {

    public AdjuntoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
