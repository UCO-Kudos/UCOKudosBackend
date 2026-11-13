package co.edu.uco.kudos.dominio.excepcion;

/**
 * El almacenamiento de archivos no respondió o rechazó el archivo. Al ser una excepción
 * no verificada, revierte la transacción de publicar. La API la traduce a 502 (problem+json).
 */
public class FalloAlmacenamientoException extends RuntimeException {

    public FalloAlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
