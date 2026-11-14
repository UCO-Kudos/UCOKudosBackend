package co.edu.uco.kudos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.uco.kudos.dominio.excepcion.ReglaDeNegocioException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KudoTest {

    private final Persona autor = persona(EstadoCuenta.ACTIVA);
    private final Persona destinatario = persona(EstadoCuenta.ACTIVA);
    private final Categoria categoriaActiva = new Categoria(UUID.randomUUID(), "Liderazgo", true);

    @Test
    void creaUnKudoValido() {
        Kudo kudo = Kudo.crear(autor, destinatario, categoriaActiva, "Gracias por tu ayuda");

        assertNotNull(kudo.id());
        assertNotNull(kudo.creadoEn());
        assertEquals(autor.id(), kudo.autorId());
        assertEquals(destinatario.id(), kudo.destinatarioId());
        assertEquals(categoriaActiva.id(), kudo.categoriaId());
        assertEquals("Gracias por tu ayuda", kudo.texto());
    }

    @Test
    void rechazaAutorIgualAlDestinatario() {
        ReglaDeNegocioException error = assertThrows(ReglaDeNegocioException.class,
                () -> Kudo.crear(autor, autor, categoriaActiva, "Me felicito"));

        assertEquals("No puedes enviarte un Kudo a ti mismo", error.getMessage());
    }

    @Test
    void rechazaCategoriaInactiva() {
        Categoria inactiva = new Categoria(UUID.randomUUID(), "Antigua", false);

        assertThrows(ReglaDeNegocioException.class,
                () -> Kudo.crear(autor, destinatario, inactiva, "Gracias"));
    }

    @Test
    void rechazaTextoVacioOEnBlanco() {
        assertThrows(ReglaDeNegocioException.class,
                () -> Kudo.crear(autor, destinatario, categoriaActiva, ""));
        assertThrows(ReglaDeNegocioException.class,
                () -> Kudo.crear(autor, destinatario, categoriaActiva, "   "));
        assertThrows(ReglaDeNegocioException.class,
                () -> Kudo.crear(autor, destinatario, categoriaActiva, null));
    }

    @Test
    void rechazaDestinatarioSinCuentaActiva() {
        Persona suspendida = persona(EstadoCuenta.SUSPENDIDA);

        assertThrows(ReglaDeNegocioException.class,
                () -> Kudo.crear(autor, suspendida, categoriaActiva, "Gracias"));
    }

    private static Persona persona(EstadoCuenta estado) {
        return new Persona(UUID.randomUUID(), "Nombre", "Apellido", "correo@uco.net.co",
                "+570000000000", estado, Rol.ESTUDIANTE);
    }
}
