package co.edu.uco.kudos.infraestructura.input.controlador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uco.kudos.servicios.input.dto.RegistroAuditoriaResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Borde HTTP de GET /api/auditoria con un caso de uso falso. El rol ADMIN lo exige SeguridadConfig. */
class AuditoriaControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        AuditoriaController controlador = new AuditoriaController(traceId -> List.of(
                registro("KUDO_PUBLICADO", traceId),
                registro("NOTIFICACION_CORREO_FALLIDA", traceId)));
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new ManejadorGlobalErrores())
                .build();
    }

    @Test
    void devuelveLasFilasDelTraceIdEnOrden() throws Exception {
        mockMvc.perform(get("/api/auditoria").param("traceId", "trace-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accion").value("KUDO_PUBLICADO"))
                .andExpect(jsonPath("$[1].accion").value("NOTIFICACION_CORREO_FALLIDA"))
                .andExpect(jsonPath("$[1].traceId").value("trace-1"));
    }

    @Test
    void sinTraceIdResponde400ProblemJson() throws Exception {
        mockMvc.perform(get("/api/auditoria"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void traceIdEnBlancoResponde400() throws Exception {
        mockMvc.perform(get("/api/auditoria").param("traceId", " "))
                .andExpect(status().isBadRequest());
    }

    private static RegistroAuditoriaResponse registro(String accion, String traceId) {
        return new RegistroAuditoriaResponse(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), accion, "KUDO",
                UUID.randomUUID(), "EXITO", traceId, Map.of());
    }
}
