package co.edu.uco.kudos.infraestructura.output.adapter.almacenamiento;

import co.edu.uco.kudos.dominio.excepcion.FalloAlmacenamientoException;
import co.edu.uco.kudos.servicios.output.port.FileStoragePort;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Sube adjuntos a un bucket privado de Supabase Storage con su API REST (sin SDK).
 * La clave de servicio solo vive en el backend y se envía en las cabeceras; nunca se registra en logs.
 */
@Component
public class SupabaseStorageAdapter implements FileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageAdapter.class);

    private final RestClient restClient;
    private final String bucket;

    public SupabaseStorageAdapter(@Value("${kudos.supabase.url}") String urlSupabase,
                                  @Value("${kudos.supabase.clave-servicio}") String claveServicio,
                                  @Value("${kudos.supabase.bucket}") String bucket) {
        // Sin timeouts, un Supabase lento dejaría la petición (y la transacción) colgada.
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(urlSupabase.replaceAll("/+$", "") + "/storage/v1")
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + claveServicio)
                .defaultHeader("apikey", claveServicio)
                .build();
        this.bucket = bucket;
    }

    @Override
    public String guardar(UUID kudoId, String tipoMime, byte[] contenido) {
        String nombreObjeto = UUID.randomUUID().toString();
        try {
            restClient.post()
                    .uri("/object/{bucket}/kudos/{kudoId}/{nombre}", bucket, kudoId, nombreObjeto)
                    .contentType(MediaType.parseMediaType(tipoMime))
                    .body(contenido)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("No se pudo subir el adjunto del kudo {} a Supabase Storage: {}", kudoId, e.getMessage());
            throw new FalloAlmacenamientoException("No se pudo guardar el adjunto", e);
        }
        return "kudos/" + kudoId + "/" + nombreObjeto;
    }
}
