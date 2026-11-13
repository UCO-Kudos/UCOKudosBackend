package co.edu.uco.kudos.infraestructura.output.adapter.seguridad;

import co.edu.uco.kudos.servicios.output.port.VerificadorClavePort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptVerificadorClaveAdapter implements VerificadorClavePort {

    private final PasswordEncoder passwordEncoder;

    public BcryptVerificadorClaveAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean coincide(String claveEnClaro, String hash) {
        return passwordEncoder.matches(claveEnClaro, hash);
    }
}
