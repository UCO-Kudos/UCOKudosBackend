package co.edu.uco.kudos.infraestructura.output.adapter.persistencia;

import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.servicios.output.port.NotificacionRepositoryPort;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class NotificacionJdbcRepository implements NotificacionRepositoryPort {

    private final JdbcClient jdbcClient;

    public NotificacionJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void guardar(Notificacion notificacion) {
        jdbcClient.sql("""
                        INSERT INTO notificacion (id, kudo_id, destinatario_id, canal, estado, intentos,
                                                  ultimo_error, creada_en, enviada_en)
                        VALUES (:id, :kudoId, :destinatarioId, :canal, :estado, :intentos,
                                :ultimoError, :creadaEn, :enviadaEn)
                        """)
                .param("id", notificacion.id())
                .param("kudoId", notificacion.kudoId())
                .param("destinatarioId", notificacion.destinatarioId())
                .param("canal", notificacion.canal().name())
                .param("estado", notificacion.estado().name())
                .param("intentos", notificacion.intentos())
                .param("ultimoError", notificacion.ultimoError())
                .param("creadaEn", aFechaConZona(notificacion.creadaEn()))
                .param("enviadaEn", aFechaConZona(notificacion.enviadaEn()))
                .update();
    }

    // El driver de PostgreSQL no acepta Instant directamente; TIMESTAMPTZ sí acepta OffsetDateTime.
    private static OffsetDateTime aFechaConZona(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }
}
