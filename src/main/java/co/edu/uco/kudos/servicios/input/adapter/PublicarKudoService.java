package co.edu.uco.kudos.servicios.input.adapter;

import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.dominio.excepcion.AdjuntoInvalidoException;
import co.edu.uco.kudos.dominio.excepcion.ReglaDeNegocioException;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Adjunto;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.dominio.modelo.Persona;
import co.edu.uco.kudos.dominio.modelo.RegistroAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.TipoAdjunto;
import co.edu.uco.kudos.servicios.input.dto.ArchivoAdjunto;
import co.edu.uco.kudos.servicios.input.dto.PublicarKudoCommand;
import co.edu.uco.kudos.servicios.input.dto.PublicarKudoResponse;
import co.edu.uco.kudos.servicios.input.port.PublicarKudoUseCase;
import co.edu.uco.kudos.servicios.output.port.AuditLogPort;
import co.edu.uco.kudos.servicios.output.port.CategoriaRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.DomainEventPublisherPort;
import co.edu.uco.kudos.servicios.output.port.FileStoragePort;
import co.edu.uco.kudos.servicios.output.port.KudoRepositoryPort;
import co.edu.uco.kudos.servicios.output.port.PersonaRepositoryPort;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Página 6 del diagrama de secuencia. Todo ocurre en una transacción: si algo falla
 * (regla de negocio, adjunto, almacenamiento, BD) no queda ni el Kudo ni su auditoría.
 * El evento KudoPublicado se publica aquí, pero las notificaciones solo arrancan tras el COMMIT.
 */
@Service
public class PublicarKudoService implements PublicarKudoUseCase {

    static final int MAXIMO_ADJUNTOS = 5;
    static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private final PersonaRepositoryPort personas;
    private final CategoriaRepositoryPort categorias;
    private final FileStoragePort almacenamiento;
    private final KudoRepositoryPort kudos;
    private final AuditLogPort auditoria;
    private final DomainEventPublisherPort eventos;

    public PublicarKudoService(PersonaRepositoryPort personas, CategoriaRepositoryPort categorias,
                               FileStoragePort almacenamiento, KudoRepositoryPort kudos,
                               AuditLogPort auditoria, DomainEventPublisherPort eventos) {
        this.personas = personas;
        this.categorias = categorias;
        this.almacenamiento = almacenamiento;
        this.kudos = kudos;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public PublicarKudoResponse publicar(PublicarKudoCommand comando) {
        Persona autor = buscarPersona(comando.autorId(), "El autor no existe");
        Persona destinatario = buscarPersona(comando.destinatarioId(), "El destinatario no existe");
        Categoria categoria = categorias.buscarPorId(comando.categoriaId())
                .orElseThrow(() -> new ReglaDeNegocioException("La categoría no existe"));

        Kudo kudo = Kudo.crear(autor, destinatario, categoria, comando.contenido());

        List<ArchivoAdjunto> archivos = comando.adjuntos() == null ? List.of() : comando.adjuntos();
        validarAdjuntos(archivos);
        List<Adjunto> adjuntos = guardarAdjuntos(kudo, archivos);
        kudos.guardar(kudo, adjuntos);

        auditoria.registrar(RegistroAuditoria.nuevo(autor.id(), AccionAuditoria.KUDO_PUBLICADO, "KUDO", kudo.id(),
                ResultadoAuditoria.EXITO, comando.traceId(),
                Map.of("categoriaId", categoria.id().toString(), "adjuntos", String.valueOf(adjuntos.size()))));
        eventos.publicar(new KudoPublicado(kudo.id(), autor.id(), destinatario.id(), comando.traceId()));

        return new PublicarKudoResponse(kudo.id(), kudo.destinatarioId(), kudo.categoriaId(), kudo.creadoEn(),
                adjuntos.size(), comando.traceId());
    }

    private Persona buscarPersona(UUID id, String mensajeSiNoExiste) {
        return personas.buscarPorId(id).orElseThrow(() -> new ReglaDeNegocioException(mensajeSiNoExiste));
    }

    // Se validan todos antes de subir el primero: así un archivo inválido no deja otros huérfanos en el bucket.
    private void validarAdjuntos(List<ArchivoAdjunto> archivos) {
        if (archivos.size() > MAXIMO_ADJUNTOS) {
            throw new AdjuntoInvalidoException("Se permiten como máximo " + MAXIMO_ADJUNTOS + " adjuntos");
        }
        for (ArchivoAdjunto archivo : archivos) {
            if (archivo.contenido() == null || archivo.contenido().length == 0) {
                throw new AdjuntoInvalidoException("El adjunto está vacío");
            }
            if (archivo.contenido().length > TAMANO_MAXIMO_BYTES) {
                throw new AdjuntoInvalidoException("Cada adjunto puede pesar como máximo 5 MB");
            }
            TipoAdjunto.detectar(archivo.contenido());
        }
    }

    private List<Adjunto> guardarAdjuntos(Kudo kudo, List<ArchivoAdjunto> archivos) {
        List<Adjunto> adjuntos = new ArrayList<>();
        for (ArchivoAdjunto archivo : archivos) {
            String tipoMime = TipoAdjunto.detectar(archivo.contenido()).tipoMime();
            String clave = almacenamiento.guardar(kudo.id(), tipoMime, archivo.contenido());
            adjuntos.add(new Adjunto(UUID.randomUUID(), sanitizarNombre(archivo.nombreOriginal()), tipoMime,
                    archivo.contenido().length, sha256(archivo.contenido()), clave));
        }
        return adjuntos;
    }

    // El nombre original solo se guarda como dato (nunca como ruta): se quitan carpetas y caracteres raros.
    static String sanitizarNombre(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "adjunto";
        }
        String sinRuta = nombreOriginal.replaceAll(".*[/\\\\]", "");
        String limpio = sinRuta.replaceAll("[^\\p{L}\\p{N}._ -]", "_");
        return limpio.length() > 200 ? limpio.substring(0, 200) : limpio;
    }

    static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no soporta SHA-256", e);
        }
    }
}
