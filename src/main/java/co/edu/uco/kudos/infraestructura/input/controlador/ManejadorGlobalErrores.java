package co.edu.uco.kudos.infraestructura.input.controlador;

import co.edu.uco.kudos.dominio.excepcion.AdjuntoInvalidoException;
import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasException;
import co.edu.uco.kudos.dominio.excepcion.FalloAlmacenamientoException;
import co.edu.uco.kudos.dominio.excepcion.ReglaDeNegocioException;
import co.edu.uco.kudos.infraestructura.configuracion.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Todos los errores salen como application/problem+json (RFC 9457) con el traceId de la petición.
 * La clase base ya convierte los errores de Spring MVC (validación, JSON mal formado, 404...);
 * aquí se agregan los nuestros. La traza de pila va al log, nunca al cliente.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ProblemDetail reglaDeNegocio(ReglaDeNegocioException error) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, error.getMessage());
    }

    @ExceptionHandler(AdjuntoInvalidoException.class)
    public ProblemDetail adjuntoInvalido(AdjuntoInvalidoException error) {
        return problema(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasException error) {
        return problema(HttpStatus.UNAUTHORIZED, error.getMessage());
    }

    @ExceptionHandler(FalloAlmacenamientoException.class)
    public ProblemDetail falloAlmacenamiento(FalloAlmacenamientoException error) {
        return problema(HttpStatus.BAD_GATEWAY, "No se pudo guardar el adjunto; intenta de nuevo");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInesperado(Exception error) {
        log.error("Error no controlado", error);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
    }

    // Punto por el que pasan las respuestas de la clase base: se les agrega el traceId.
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object cuerpo, HttpHeaders cabeceras,
                                                          HttpStatusCode estado, WebRequest peticion) {
        if (cuerpo instanceof ProblemDetail problema) {
            problema.setProperty("traceId", TraceIdFilter.actual());
        }
        return super.createResponseEntity(cuerpo, cabeceras, estado, peticion);
    }

    private static ProblemDetail problema(HttpStatus estado, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setProperty("traceId", TraceIdFilter.actual());
        return problema;
    }
}
