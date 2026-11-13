package co.edu.uco.kudos.infraestructura.output.adapter.persistencia;

import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.servicios.output.port.CategoriaRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CategoriaJdbcRepository implements CategoriaRepositoryPort {

    private final JdbcClient jdbcClient;

    public CategoriaJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<Categoria> buscarPorId(UUID id) {
        return jdbcClient.sql("SELECT id, nombre, activa FROM categoria WHERE id = :id")
                .param("id", id)
                .query(this::mapear)
                .optional();
    }

    @Override
    public List<Categoria> listarActivas() {
        return jdbcClient.sql("SELECT id, nombre, activa FROM categoria WHERE activa ORDER BY nombre")
                .query(this::mapear)
                .list();
    }

    private Categoria mapear(ResultSet fila, int numeroFila) throws SQLException {
        return new Categoria(
                fila.getObject("id", UUID.class),
                fila.getString("nombre"),
                fila.getBoolean("activa"));
    }
}
