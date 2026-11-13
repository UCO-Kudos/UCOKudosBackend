package co.edu.uco.kudos.infraestructura.output.adapter.persistencia;

import co.edu.uco.kudos.dominio.modelo.EstadoCuenta;
import co.edu.uco.kudos.dominio.modelo.Persona;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.servicios.output.port.PersonaRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PersonaJdbcRepository implements PersonaRepositoryPort {

    // Se une con el rol activo (RN-01: una persona tiene a lo sumo uno).
    private static final String SELECT_PERSONA = """
            SELECT p.id, p.nombres, p.apellidos, p.correo, p.celular, p.estado_cuenta, r.rol
            FROM persona p
            JOIN asignacion_rol r ON r.persona_id = p.id AND r.activo
            """;

    private final JdbcClient jdbcClient;

    public PersonaJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<Persona> buscarPorId(UUID id) {
        return jdbcClient.sql(SELECT_PERSONA + "WHERE p.id = :id")
                .param("id", id)
                .query(this::mapear)
                .optional();
    }

    @Override
    public Optional<Persona> buscarPorCorreo(String correo) {
        return jdbcClient.sql(SELECT_PERSONA + "WHERE p.correo = :correo")
                .param("correo", correo)
                .query(this::mapear)
                .optional();
    }

    @Override
    public Optional<String> buscarHashClave(UUID personaId) {
        return jdbcClient.sql("SELECT hash_clave FROM persona WHERE id = :id")
                .param("id", personaId)
                .query(String.class)
                .optional();
    }

    @Override
    public List<Persona> listarActivasExcepto(UUID personaId) {
        return jdbcClient.sql(SELECT_PERSONA + """
                        WHERE p.estado_cuenta = 'ACTIVA' AND p.id <> :id
                        ORDER BY p.nombres, p.apellidos
                        """)
                .param("id", personaId)
                .query(this::mapear)
                .list();
    }

    private Persona mapear(ResultSet fila, int numeroFila) throws SQLException {
        return new Persona(
                fila.getObject("id", UUID.class),
                fila.getString("nombres"),
                fila.getString("apellidos"),
                fila.getString("correo"),
                fila.getString("celular"),
                EstadoCuenta.valueOf(fila.getString("estado_cuenta")),
                Rol.valueOf(fila.getString("rol")));
    }
}
