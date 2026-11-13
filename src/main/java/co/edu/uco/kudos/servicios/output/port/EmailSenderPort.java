package co.edu.uco.kudos.servicios.output.port;

public interface EmailSenderPort {

    /** Lanza una excepción si el proveedor no acepta el correo. */
    void enviar(String para, String asunto, String cuerpo);
}
