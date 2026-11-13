package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.ReglaDeNegocioException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Reconocimiento de una persona a otra. El constructor es privado: la única forma
 * de obtener un Kudo es {@link #crear}, que garantiza que nace válido.
 */
public final class Kudo {

    private final UUID id;
    private final UUID autorId;
    private final UUID destinatarioId;
    private final UUID categoriaId;
    private final String texto;
    private final Instant creadoEn;

    private Kudo(UUID id, UUID autorId, UUID destinatarioId, UUID categoriaId, String texto, Instant creadoEn) {
        this.id = id;
        this.autorId = autorId;
        this.destinatarioId = destinatarioId;
        this.categoriaId = categoriaId;
        this.texto = texto;
        this.creadoEn = creadoEn;
    }

    public static Kudo crear(Persona autor, Persona destinatario, Categoria categoria, String texto) {
        Objects.requireNonNull(autor, "autor");
        Objects.requireNonNull(destinatario, "destinatario");
        Objects.requireNonNull(categoria, "categoria");

        if (!autor.estaActiva()) {
            throw new ReglaDeNegocioException("El autor no tiene una cuenta activa");
        }
        if (!destinatario.estaActiva()) {
            throw new ReglaDeNegocioException("El destinatario no tiene una cuenta activa");
        }
        if (autor.id().equals(destinatario.id())) {
            throw new ReglaDeNegocioException("No puedes enviarte un Kudo a ti mismo");
        }
        if (!categoria.activa()) {
            throw new ReglaDeNegocioException("La categoría no está activa");
        }
        if (texto == null || texto.isBlank()) {
            throw new ReglaDeNegocioException("El contenido del Kudo no puede estar vacío");
        }
        return new Kudo(UUID.randomUUID(), autor.id(), destinatario.id(), categoria.id(), texto, Instant.now());
    }

    public UUID id() {
        return id;
    }

    public UUID autorId() {
        return autorId;
    }

    public UUID destinatarioId() {
        return destinatarioId;
    }

    public UUID categoriaId() {
        return categoriaId;
    }

    public String texto() {
        return texto;
    }

    public Instant creadoEn() {
        return creadoEn;
    }
}
