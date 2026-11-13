package co.edu.uco.kudos.servicios.output.port;

import co.edu.uco.kudos.dominio.evento.KudoPublicado;

public interface DomainEventPublisherPort {

    void publicar(KudoPublicado evento);
}
