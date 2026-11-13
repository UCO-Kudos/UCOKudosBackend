package co.edu.uco.kudos.servicios.input.port;

import co.edu.uco.kudos.dominio.evento.KudoPublicado;

public interface NotificarKudoPublicadoUseCase {

    void notificar(KudoPublicado evento);
}
