package co.edu.uco.kudos.servicios.output.port;

public interface WhatsAppSenderPort {

    /** celular en formato E.164 (+57...). Lanza una excepción si el proveedor no acepta el mensaje. */
    void enviar(String celular, String mensaje);
}
