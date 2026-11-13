package co.edu.uco.kudos.infraestructura.output.adapter.seguridad;

import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.servicios.output.port.TokenServicePort;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite el JWT HS256 con Nimbus (JwtEncoder de Spring Security). Solo lleva sub, rol, iat y exp:
 * nada sensible, porque el contenido de un JWT se puede leer sin la clave.
 */
@Component
public class JwtTokenAdapter implements TokenServicePort {

    public static final String CLAIM_ROL = "rol";
    private static final String EMISOR = "uco-kudos";

    private final JwtEncoder jwtEncoder;
    private final Duration vigencia;

    public JwtTokenAdapter(JwtEncoder jwtEncoder, @Value("${kudos.jwt.minutos-vigencia:60}") long minutosVigencia) {
        this.jwtEncoder = jwtEncoder;
        this.vigencia = Duration.ofMinutes(minutosVigencia);
    }

    @Override
    public String emitir(UUID personaId, Rol rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(personaId.toString())
                .claim(CLAIM_ROL, rol.name())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
