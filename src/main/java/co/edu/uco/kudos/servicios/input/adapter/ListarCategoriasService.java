package co.edu.uco.kudos.servicios.input.adapter;

import co.edu.uco.kudos.servicios.input.dto.CategoriaResponse;
import co.edu.uco.kudos.servicios.input.port.ListarCategoriasUseCase;
import co.edu.uco.kudos.servicios.output.port.CategoriaRepositoryPort;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListarCategoriasService implements ListarCategoriasUseCase {

    private final CategoriaRepositoryPort categorias;

    public ListarCategoriasService(CategoriaRepositoryPort categorias) {
        this.categorias = categorias;
    }

    @Override
    public List<CategoriaResponse> listar() {
        return categorias.listarActivas().stream()
                .map(categoria -> new CategoriaResponse(categoria.id(), categoria.nombre()))
                .toList();
    }
}
