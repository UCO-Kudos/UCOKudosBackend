package co.edu.uco.kudos.dominio.excepcion;

/**
 * Un canal de notificación (correo o WhatsApp) no pudo entregar el mensaje: el proveedor falló,
 * el circuito está abierto o el canal no tiene credenciales. Lleva cuántos intentos se hicieron
 * (0 si ni siquiera se llamó al proveedor) para guardarlo en la tabla notificacion.
 */
public class EnvioFallidoException extends RuntimeException {

    private final int intentos;

    public EnvioFallidoException(String mensaje, int intentos) {
        super(mensaje);
        this.intentos = intentos;
    }

    public EnvioFallidoException(String mensaje, int intentos, Throwable causa) {
        super(mensaje, causa);
        this.intentos = intentos;
    }

    public int intentos() {
        return intentos;
    }
}
