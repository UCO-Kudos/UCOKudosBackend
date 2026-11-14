package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.ReglaDeNegocioException;
import java.util.Locale;

/**
 * Regla del enunciado: toda Persona usa su correo institucional, es decir, uno que termina
 * en "@" + dominio (p. ej. autor@uco.net.co). El dominio es configurable (DOMINIO_INSTITUCIONAL),
 * por eso es un valor que se le entrega al dominio y no una constante.
 * Se compara sin distinguir mayúsculas y exigiendo la "@": "x@otro-uco.net.co" no es institucional.
 */
public record DominioInstitucional(String valor) {

    public DominioInstitucional {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El dominio institucional es obligatorio");
        }
        valor = valor.strip().toLowerCase(Locale.ROOT);
        if (valor.startsWith("@")) {
            valor = valor.substring(1);
        }
        if (valor.isEmpty() || valor.contains("@") || !valor.contains(".")) {
            throw new IllegalArgumentException("Dominio institucional no válido: " + valor);
        }
    }

    public boolean admite(String correo) {
        if (correo == null) {
            return false;
        }
        String normalizado = correo.strip().toLowerCase(Locale.ROOT);
        String sufijo = "@" + valor;
        return normalizado.endsWith(sufijo) && normalizado.length() > sufijo.length();
    }

    public boolean admite(Persona persona) {
        return admite(persona.correo());
    }

    /** Lanza ReglaDeNegocioException si el correo no es del dominio institucional. */
    public void exigir(String correo) {
        if (!admite(correo)) {
            throw new ReglaDeNegocioException("El correo debe ser institucional (@" + valor + ")");
        }
    }
}
