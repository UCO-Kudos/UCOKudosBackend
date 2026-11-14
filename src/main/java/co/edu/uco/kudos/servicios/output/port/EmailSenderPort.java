package co.edu.uco.kudos.servicios.output.port;

public interface EmailSenderPort {

    /**
     * @return cuántos intentos hicieron falta para que el proveedor aceptara el correo.
     * Lanza EnvioFallidoException (con los intentos hechos) si no se pudo enviar.
     */
    int enviar(String para, String asunto, String cuerpo);
}
