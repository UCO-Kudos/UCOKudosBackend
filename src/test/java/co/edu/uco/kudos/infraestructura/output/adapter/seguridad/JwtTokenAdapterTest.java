package co.edu.uco.kudos.infraestructura.output.adapter.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.infraestructura.configuracion.SeguridadConfig;
import java.time.Duration;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Usa los mismos beans de SeguridadConfig que la aplicación: lo que se emite en el login
 * es exactamente lo que el filtro JWT acepta después.
 */
class JwtTokenAdapterTest {

    private final SeguridadConfig config = new SeguridadConfig();
    private final SecretKey clave = config.claveJwt("clave-de-prueba-con-al-menos-32-bytes!");

    @Test
    void emiteUnTokenQueElDecodificadorAcepta() {
        UUID personaId = UUID.randomUUID();
        String token = new JwtTokenAdapter(config.jwtEncoder(clave), 60).emitir(personaId, Rol.ADMIN);

        Jwt jwt = config.jwtDecoder(clave).decode(token);

        assertEquals(personaId.toString(), jwt.getSubject());
        assertEquals("ADMIN", jwt.getClaimAsString("rol"));
        assertEquals(Duration.ofMinutes(60), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        SecretKey otraClave = config.claveJwt("otra-clave-distinta-con-al-menos-32-bytes");
        String token = new JwtTokenAdapter(config.jwtEncoder(otraClave), 60).emitir(UUID.randomUUID(), Rol.ESTUDIANTE);

        assertThrows(JwtException.class, () -> config.jwtDecoder(clave).decode(token));
    }

    @Test
    void noArrancaConUnSecretoCorto() {
        assertThrows(IllegalStateException.class, () -> config.claveJwt("corto"));
    }
}
