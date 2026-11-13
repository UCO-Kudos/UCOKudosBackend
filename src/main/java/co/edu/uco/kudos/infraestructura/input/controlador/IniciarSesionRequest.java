package co.edu.uco.kudos.infraestructura.input.controlador;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record IniciarSesionRequest(@NotBlank @Email String correo, @NotBlank String clave) {

    // Que la clave nunca termine en un log si alguien imprime la petición.
    @Override
    public String toString() {
        return "IniciarSesionRequest[correo=" + correo + ", clave=****]";
    }
}
