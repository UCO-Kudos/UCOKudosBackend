package co.edu.uco.kudos.dominio.excepcion;

/**
 * Correo o clave incorrectos, o cuenta no activa. El mensaje es siempre el mismo
 * para no revelar si el correo existe. La API la traduce a 401 (problem+json).
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas");
    }
}
