package co.edu.uco.kudos.servicios.input.dto;

public record IniciarSesionCommand(String correo, String clave, String traceId) {
}
