package co.edu.uco.kudos.servicios.input.dto;

import java.time.Instant;
import java.util.UUID;

/** direccion: "RECIBIDO" si el usuario es el destinatario del Kudo, "ENVIADO" si es el autor. */
public record KudoResumenResponse(UUID id, String direccion, String autor, String destinatario,
                                  String categoria, String texto, Instant creadoEn) {
}
