package co.edu.uco.kudos.infraestructura.configuracion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los 401 y 403 ocurren en los filtros de seguridad, antes de llegar a los controladores,
 * así que el @RestControllerAdvice no los ve. Este manejador les da el mismo formato problem+json con traceId.
 */
@Component
public class ErroresSeguridadHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public ErroresSeguridadHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
                         AuthenticationException error) throws IOException {
        respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(respuesta, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión con un token válido");
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
                       AccessDeniedException error) throws IOException {
        escribir(respuesta, HttpStatus.FORBIDDEN, "No tienes permiso para esta operación");
    }

    private void escribir(HttpServletResponse respuesta, HttpStatus estado, String detalle) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setProperty("traceId", TraceIdFilter.actual());
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(respuesta.getOutputStream(), problema);
    }
}
