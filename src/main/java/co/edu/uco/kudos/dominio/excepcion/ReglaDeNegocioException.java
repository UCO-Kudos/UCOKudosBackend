package co.edu.uco.kudos.dominio.excepcion;

/**
 * Una regla de negocio no se cumple. La API la traduce a 422 (problem+json).
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
