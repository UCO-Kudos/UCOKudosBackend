package co.edu.uco.kudos.servicios.input.port;

import co.edu.uco.kudos.servicios.input.dto.CategoriaResponse;
import java.util.List;

public interface ListarCategoriasUseCase {

    List<CategoriaResponse> listar();
}
