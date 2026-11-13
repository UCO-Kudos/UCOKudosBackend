package co.edu.uco.kudos.infraestructura.input.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uco.kudos.dominio.excepcion.ReglaDeNegocioException;
import co.edu.uco.kudos.servicios.input.dto.PublicarKudoCommand;
import co.edu.uco.kudos.servicios.input.dto.PublicarKudoResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Prueba solo el borde HTTP (multipart, Bean Validation, códigos y problem+json),
 * con un caso de uso falso y sin levantar Spring ni la base de datos.
 */
class KudoControllerTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1};

    private final UUID autorId = UUID.randomUUID();
    private PublicarKudoCommand comandoRecibido;
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        KudoController controlador = new KudoController(comando -> {
            comandoRecibido = comando;
            if (comando.contenido().equals("auto")) {
                throw new ReglaDeNegocioException("No puedes enviarte un Kudo a ti mismo");
            }
            return new PublicarKudoResponse(UUID.randomUUID(), comando.destinatarioId(), comando.categoriaId(),
                    Instant.now(), comando.adjuntos().size(), comando.traceId());
        });
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new ManejadorGlobalErrores())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256")
                .subject(autorId.toString()).claim("rol", "ESTUDIANTE").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicaConAdjuntoYResponde201() throws Exception {
        mockMvc.perform(multipart("/api/kudos")
                        .file(new MockMultipartFile("adjuntos", "foto.png", "image/png", PNG))
                        .param("destinatarioId", UUID.randomUUID().toString())
                        .param("categoriaId", UUID.randomUUID().toString())
                        .param("contenido", "Gracias por tu ayuda"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjuntos").value(1));

        assertEquals(autorId, comandoRecibido.autorId());
        assertEquals("foto.png", comandoRecibido.adjuntos().getFirst().nombreOriginal());
    }

    @Test
    void sinDestinatarioResponde400ProblemJson() throws Exception {
        mockMvc.perform(multipart("/api/kudos")
                        .param("categoriaId", UUID.randomUUID().toString())
                        .param("contenido", "Gracias"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void reglaDeNegocioResponde422ProblemJson() throws Exception {
        mockMvc.perform(multipart("/api/kudos")
                        .param("destinatarioId", UUID.randomUUID().toString())
                        .param("categoriaId", UUID.randomUUID().toString())
                        .param("contenido", "auto"))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No puedes enviarte un Kudo a ti mismo"));
    }
}
