package co.edu.uco.kudos.infraestructura.output.adapter.persistencia;

import co.edu.uco.kudos.dominio.modelo.Adjunto;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.dominio.modelo.KudoDetalle;
import co.edu.uco.kudos.servicios.output.port.KudoRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Un Kudo es una fila de contenido con tipo 'KUDO'. Corre dentro de la transacción
 * del caso de uso: si falla un adjunto, tampoco queda el Kudo.
 */
@Repository
public class KudoJdbcRepository implements KudoRepositoryPort {

    private final JdbcClient jdbcClient;

    public KudoJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void guardar(Kudo kudo, List<Adjunto> adjuntos) {
        jdbcClient.sql("""
                        INSERT INTO contenido (id, tipo, autor_id, texto, creado_en, destinatario_id, categoria_id)
                        VALUES (:id, 'KUDO', :autorId, :texto, :creadoEn, :destinatarioId, :categoriaId)
                        """)
                .param("id", kudo.id())
                .param("autorId", kudo.autorId())
                .param("texto", kudo.texto())
                .param("creadoEn", kudo.creadoEn().atOffset(ZoneOffset.UTC))
                .param("destinatarioId", kudo.destinatarioId())
                .param("categoriaId", kudo.categoriaId())
                .update();

        for (Adjunto adjunto : adjuntos) {
            guardarAdjunto(kudo, adjunto);
        }
    }

    @Override
    public List<KudoDetalle> listarDePersona(UUID personaId) {
        return jdbcClient.sql("""
                        SELECT c.id, c.autor_id, a.nombres || ' ' || a.apellidos AS autor_nombre,
                               c.destinatario_id, d.nombres || ' ' || d.apellidos AS destinatario_nombre,
                               cat.nombre AS categoria, c.texto, c.creado_en
                        FROM contenido c
                        JOIN persona a ON a.id = c.autor_id
                        JOIN persona d ON d.id = c.destinatario_id
                        JOIN categoria cat ON cat.id = c.categoria_id
                        WHERE c.tipo = 'KUDO' AND c.estado = 'ACTIVO'
                          AND (c.autor_id = :personaId OR c.destinatario_id = :personaId)
                        ORDER BY c.creado_en DESC
                        LIMIT 50
                        """)
                .param("personaId", personaId)
                .query(this::mapearDetalle)
                .list();
    }

    private KudoDetalle mapearDetalle(ResultSet fila, int numeroFila) throws SQLException {
        return new KudoDetalle(
                fila.getObject("id", UUID.class),
                fila.getObject("autor_id", UUID.class),
                fila.getString("autor_nombre"),
                fila.getObject("destinatario_id", UUID.class),
                fila.getString("destinatario_nombre"),
                fila.getString("categoria"),
                fila.getString("texto"),
                fila.getObject("creado_en", OffsetDateTime.class).toInstant());
    }

    private void guardarAdjunto(Kudo kudo, Adjunto adjunto) {
        jdbcClient.sql("""
                        INSERT INTO adjunto (id, kudo_id, nombre_original, tipo_mime, tamano_bytes,
                                             sha256, clave_almacenamiento, subido_por)
                        VALUES (:id, :kudoId, :nombreOriginal, :tipoMime, :tamanoBytes,
                                :sha256, :claveAlmacenamiento, :subidoPor)
                        """)
                .param("id", adjunto.id())
                .param("kudoId", kudo.id())
                .param("nombreOriginal", adjunto.nombreOriginal())
                .param("tipoMime", adjunto.tipoMime())
                .param("tamanoBytes", adjunto.tamanoBytes())
                .param("sha256", adjunto.sha256())
                .param("claveAlmacenamiento", adjunto.claveAlmacenamiento())
                .param("subidoPor", kudo.autorId())
                .update();
    }
}
