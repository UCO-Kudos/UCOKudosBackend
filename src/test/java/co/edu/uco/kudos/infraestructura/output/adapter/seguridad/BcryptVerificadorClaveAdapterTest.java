package co.edu.uco.kudos.infraestructura.output.adapter.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BcryptVerificadorClaveAdapterTest {

    // Hash de la semilla V2 para la clave de demostración Kudos2026*
    private static final String HASH_SEMILLA = "$2a$10$OxR1g59g35HAQOX2ozoMU.oJ3k3sBchpMmeunqFojXb5MW.GmaBh6";

    private final BcryptVerificadorClaveAdapter verificador =
            new BcryptVerificadorClaveAdapter(new BCryptPasswordEncoder());

    @Test
    void laClaveDeLaSemillaCoincide() {
        assertTrue(verificador.coincide("Kudos2026*", HASH_SEMILLA));
    }

    @Test
    void unaClaveDistintaNoCoincide() {
        assertFalse(verificador.coincide("kudos2026*", HASH_SEMILLA));
    }
}
