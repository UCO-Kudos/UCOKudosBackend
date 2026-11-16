package co.edu.uco.kudos.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Vista de lectura de un Kudo ya publicado, con los nombres del autor, del destinatario
 * y de la categoría resueltos. No tiene invariantes: los garantizó {@link Kudo#crear} al publicarlo.
 */
public record KudoDetalle(UUID id, UUID autorId, String autorNombre, UUID destinatarioId,
                          String destinatarioNombre, String categoria, String texto, Instant creadoEn) {
}
