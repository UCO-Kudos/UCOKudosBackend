package co.edu.uco.kudos.servicios.output.port;

public interface VerificadorClavePort {

    /** Compara la clave escrita por el usuario con el hash guardado (BCrypt). */
    boolean coincide(String claveEnClaro, String hash);
}
