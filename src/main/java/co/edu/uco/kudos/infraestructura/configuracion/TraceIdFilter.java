package co.edu.uco.kudos.infraestructura.configuracion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Crea un trace_id por petición, lo pone en el MDC (así sale en cada línea del log JSON)
 * y lo devuelve en la cabecera X-Trace-Id. Va antes que Spring Security para que también
 * los 401/403 lleven traceId. No se acepta uno enviado por el cliente: lo genera el servidor.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String CLAVE_MDC = "traceId";
    public static final String CABECERA = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(CLAVE_MDC, traceId);
        respuesta.setHeader(CABECERA, traceId);
        try {
            cadena.doFilter(peticion, respuesta);
        } finally {
            MDC.remove(CLAVE_MDC);
        }
    }

    /** El trace_id de la petición en curso (o null fuera de una petición). */
    public static String actual() {
        return MDC.get(CLAVE_MDC);
    }
}
