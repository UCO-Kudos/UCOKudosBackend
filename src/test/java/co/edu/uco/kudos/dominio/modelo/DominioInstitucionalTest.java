package co.edu.uco.kudos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uco.kudos.dominio.excepcion.ReglaDeNegocioException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DominioInstitucionalTest {

    private final DominioInstitucional uco = new DominioInstitucional("uco.net.co");

    @Test
    void admiteCorreosDelDominioSinImportarMayusculasNiEspacios() {
        assertTrue(uco.admite("autor@uco.net.co"));
        assertTrue(uco.admite("  Autor@UCO.NET.CO "));
    }

    @Test
    void rechazaOtrosDominiosYParecidos() {
        assertFalse(uco.admite("autor@gmail.com"));
        assertFalse(uco.admite("autor@uco.edu.co"));
        assertFalse(uco.admite("autor@falso-uco.net.co"), "Debe exigir la @ antes del dominio");
        assertFalse(uco.admite("autor@uco.net.co.evil.com"));
        assertFalse(uco.admite("@uco.net.co"), "Falta la parte local");
        assertFalse(uco.admite((String) null));
    }

    @Test
    void validaElCorreoDeUnaPersona() {
        Persona institucional = persona("docente@uco.net.co");
        Persona externa = persona("docente@hotmail.com");

        assertTrue(uco.admite(institucional));
        assertFalse(uco.admite(externa));
    }

    @Test
    void exigirLanzaReglaDeNegocioConElDominioEsperado() {
        assertDoesNotThrow(() -> uco.exigir("autor@uco.net.co"));

        ReglaDeNegocioException error = assertThrows(ReglaDeNegocioException.class,
                () -> uco.exigir("autor@gmail.com"));
        assertEquals("El correo debe ser institucional (@uco.net.co)", error.getMessage());
    }

    @Test
    void elDominioConfiguradoSeNormalizaYDebeSerValido() {
        assertEquals("uco.net.co", new DominioInstitucional(" @UCO.net.co ").valor());
        assertThrows(IllegalArgumentException.class, () -> new DominioInstitucional(""));
        assertThrows(IllegalArgumentException.class, () -> new DominioInstitucional(null));
        assertThrows(IllegalArgumentException.class, () -> new DominioInstitucional("uco@net.co"));
        assertThrows(IllegalArgumentException.class, () -> new DominioInstitucional("localhost"));
    }

    private static Persona persona(String correo) {
        return new Persona(UUID.randomUUID(), "Nombre", "Apellido", correo, "+570000000000",
                EstadoCuenta.ACTIVA, Rol.DOCENTE);
    }
}
