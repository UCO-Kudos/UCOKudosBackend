package co.edu.uco.kudos.servicios.input.dto;

import java.util.UUID;

public record IniciarSesionResponse(String token, UUID personaId, String nombre, String rol) {
}
