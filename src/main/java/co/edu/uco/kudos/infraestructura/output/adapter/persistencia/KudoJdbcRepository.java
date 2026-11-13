package co.edu.uco.kudos.infraestructura.output.adapter.persistencia;

import co.edu.uco.kudos.dominio.modelo.Adjunto;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.servicios.output.port.KudoRepositoryPort;
import java.time.ZoneOffset;
import java.util.List;
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
