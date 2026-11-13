package co.edu.uco.kudos.infraestructura.output.adapter.auditoria;

import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.RegistroAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.servicios.output.port.AuditLogPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Solo INSERT y SELECT: la tabla además tiene un trigger que rechaza UPDATE y DELETE.
 * El trace_id se recibe del caso de uso; este adaptador no lo inventa.
 */
@Repository
public class JdbcAuditLogAdapter implements AuditLogPort {

    private static final TypeReference<Map<String, String>> TIPO_DETALLE = new TypeReference<>() { };

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    public JdbcAuditLogAdapter(JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void registrar(RegistroAuditoria registro) {
        jdbcClient.sql("""
                        INSERT INTO auditoria (id, ocurrido_en, actor_id, accion, recurso_tipo, recurso_id,
                                               resultado, trace_id, detalle)
                        VALUES (:id, :ocurridoEn, :actorId, :accion, :recursoTipo, :recursoId,
                                :resultado, :traceId, CAST(:detalle AS jsonb))
                        """)
                .param("id", registro.id())
                .param("ocurridoEn", registro.ocurridoEn().atOffset(ZoneOffset.UTC))
                .param("actorId", registro.actorId())
                .param("accion", registro.accion().name())
                .param("recursoTipo", registro.recursoTipo())
                .param("recursoId", registro.recursoId())
                .param("resultado", registro.resultado().name())
                .param("traceId", registro.traceId())
                .param("detalle", aJson(registro.detalle()))
                .update();
    }

    @Override
    public List<RegistroAuditoria> buscarPorTraceId(String traceId) {
        return jdbcClient.sql("""
                        SELECT id, ocurrido_en, actor_id, accion, recurso_tipo, recurso_id,
                               resultado, trace_id, detalle::text AS detalle
                        FROM auditoria
                        WHERE trace_id = :traceId
                        ORDER BY ocurrido_en
                        """)
                .param("traceId", traceId)
                .query(this::mapear)
                .list();
    }

    private RegistroAuditoria mapear(ResultSet fila, int numeroFila) throws SQLException {
        return new RegistroAuditoria(
                fila.getObject("id", UUID.class),
                fila.getObject("ocurrido_en", OffsetDateTime.class).toInstant(),
                fila.getObject("actor_id", UUID.class),
                AccionAuditoria.valueOf(fila.getString("accion")),
                fila.getString("recurso_tipo"),
                fila.getObject("recurso_id", UUID.class),
                ResultadoAuditoria.valueOf(fila.getString("resultado")),
                fila.getString("trace_id"),
                desdeJson(fila.getString("detalle")));
    }

    private String aJson(Map<String, String> detalle) {
        return detalle == null ? null : jsonMapper.writeValueAsString(detalle);
    }

    private Map<String, String> desdeJson(String detalle) {
        return detalle == null ? Map.of() : jsonMapper.readValue(detalle, TIPO_DETALLE);
    }
}
