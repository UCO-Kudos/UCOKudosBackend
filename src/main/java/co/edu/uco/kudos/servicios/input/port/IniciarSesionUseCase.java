package co.edu.uco.kudos.servicios.input.port;

import co.edu.uco.kudos.servicios.input.dto.IniciarSesionCommand;
import co.edu.uco.kudos.servicios.input.dto.IniciarSesionResponse;

public interface IniciarSesionUseCase {

    IniciarSesionResponse iniciarSesion(IniciarSesionCommand comando);
}
