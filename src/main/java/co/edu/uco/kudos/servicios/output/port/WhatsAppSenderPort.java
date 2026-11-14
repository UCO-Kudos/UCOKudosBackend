package co.edu.uco.kudos.servicios.output.port;

public interface WhatsAppSenderPort {

    /**
     * celular en formato E.164 (+57...).
     * @return cuántos intentos hicieron falta para que el proveedor aceptara el mensaje.
     * Lanza EnvioFallidoException (con los intentos hechos) si no se pudo enviar.
     */
    int enviar(String celular, String mensaje);
}
