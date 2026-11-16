package co.edu.uco.kudos.servicios.input.adapter;

import co.edu.uco.kudos.dominio.modelo.KudoDetalle;
import co.edu.uco.kudos.servicios.input.dto.KudoResumenResponse;
import co.edu.uco.kudos.servicios.input.port.ConsultarKudosUseCase;
import co.edu.uco.kudos.servicios.output.port.KudoRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConsultarKudosService implements ConsultarKudosUseCase {

    private final KudoRepositoryPort kudos;

    public ConsultarKudosService(KudoRepositoryPort kudos) {
        this.kudos = kudos;
    }

    @Override
    public List<KudoResumenResponse> listar(UUID usuarioId) {
        return kudos.listarDePersona(usuarioId).stream()
                .map(kudo -> aResumen(kudo, usuarioId))
                .toList();
    }

    // El repositorio solo devuelve Kudos donde el usuario es autor o destinatario,
    // y nadie se envía Kudos a sí mismo: si no es el destinatario, es el autor.
    private static KudoResumenResponse aResumen(KudoDetalle kudo, UUID usuarioId) {
        String direccion = kudo.destinatarioId().equals(usuarioId) ? "RECIBIDO" : "ENVIADO";
        return new KudoResumenResponse(kudo.id(), direccion, kudo.autorNombre(), kudo.destinatarioNombre(),
                kudo.categoria(), kudo.texto(), kudo.creadoEn());
    }
}
