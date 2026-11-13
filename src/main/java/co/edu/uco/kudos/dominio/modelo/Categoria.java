package co.edu.uco.kudos.dominio.modelo;

import java.util.UUID;

public record Categoria(UUID id, String nombre, boolean activa) {
}
