package co.edu.uco.kudos.dominio.modelo;

import java.util.UUID;

/**
 * Persona con su rol activo. El hash de la clave no viaja aquí a propósito:
 * así nunca termina en un log, un DTO o una respuesta por accidente.
 */
public record Persona(
        UUID id,
        String nombres,
        String apellidos,
        String correo,
        String celular,
        EstadoCuenta estadoCuenta,
        Rol rol) {

    public boolean estaActiva() {
        return estadoCuenta == EstadoCuenta.ACTIVA;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }
}
