package co.edu.uco.kudos.infraestructura.configuracion;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

/**
 * Verifica que una línea de log escrita con el traceId en el MDC sale en JSON (formato ECS,
 * el mismo de logging.structured.format.console) con el campo traceId.
 */
class TraceIdEnLogsJsonTest {

    @Test
    void elTraceIdDelMdcApareceEnLaLineaJson() {
        LoggerContext contexto = new LoggerContext();
        contexto.setMDCAdapter(MDC.getMDCAdapter());
        contexto.putObject(Environment.class.getName(), new MockEnvironment());

        StructuredLogEncoder codificador = new StructuredLogEncoder();
        codificador.setContext(contexto);
        codificador.setFormat("ecs");
        codificador.start();

        ListAppender<ILoggingEvent> capturados = new ListAppender<>();
        capturados.setContext(contexto);
        capturados.start();
        Logger logger = contexto.getLogger("prueba");
        logger.addAppender(capturados);

        String json;
        MDC.put(TraceIdFilter.CLAVE_MDC, "trace-123");
        try {
            logger.info("Notificación CORREO del kudo: ENVIADA");
            // Se codifica mientras el MDC sigue puesto, igual que hace el appender de consola al escribir.
            json = new String(codificador.encode(capturados.list.getFirst()), StandardCharsets.UTF_8);
        } finally {
            MDC.remove(TraceIdFilter.CLAVE_MDC);
        }

        assertTrue(json.startsWith("{"), json);
        assertTrue(json.contains("\"traceId\":\"trace-123\""), json);
    }
}
