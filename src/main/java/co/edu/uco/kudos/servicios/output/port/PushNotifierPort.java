package co.edu.uco.kudos.servicios.output.port;

import java.util.UUID;

public interface PushNotifierPort {

    /** @return false si el destinatario no tiene una conexión abierta (no es un error). */
    boolean enviar(UUID destinatarioId, UUID kudoId, String mensaje);
}
