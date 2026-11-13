package co.edu.uco.kudos.servicios.output.port;

import co.edu.uco.kudos.dominio.modelo.Categoria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepositoryPort {

    Optional<Categoria> buscarPorId(UUID id);

    List<Categoria> listarActivas();
}
