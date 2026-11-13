package co.edu.uco.kudos.servicios.input.port;

import co.edu.uco.kudos.servicios.input.dto.PublicarKudoCommand;
import co.edu.uco.kudos.servicios.input.dto.PublicarKudoResponse;

public interface PublicarKudoUseCase {

    PublicarKudoResponse publicar(PublicarKudoCommand comando);
}
