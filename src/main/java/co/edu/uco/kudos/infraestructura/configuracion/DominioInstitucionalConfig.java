package co.edu.uco.kudos.infraestructura.configuracion;

import co.edu.uco.kudos.dominio.modelo.DominioInstitucional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lee DOMINIO_INSTITUCIONAL (por defecto uco.net.co) y lo entrega al dominio como un objeto
 * sin Spring. Si el valor no es un dominio válido, la aplicación no arranca.
 */
@Configuration
public class DominioInstitucionalConfig {

    @Bean
    public DominioInstitucional dominioInstitucional(@Value("${kudos.dominio-institucional}") String dominio) {
        return new DominioInstitucional(dominio);
    }
}
