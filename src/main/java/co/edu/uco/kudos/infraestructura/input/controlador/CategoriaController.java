package co.edu.uco.kudos.infraestructura.input.controlador;

import co.edu.uco.kudos.servicios.input.dto.CategoriaResponse;
import co.edu.uco.kudos.servicios.input.port.ListarCategoriasUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final ListarCategoriasUseCase listarCategorias;

    public CategoriaController(ListarCategoriasUseCase listarCategorias) {
        this.listarCategorias = listarCategorias;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return listarCategorias.listar();
    }
}
