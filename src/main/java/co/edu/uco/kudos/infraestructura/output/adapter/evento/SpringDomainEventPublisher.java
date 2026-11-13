package co.edu.uco.kudos.infraestructura.output.adapter.evento;

import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.servicios.output.port.DomainEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publica el evento con los eventos de Spring. Como el listener será
 * @TransactionalEventListener(AFTER_COMMIT), Spring retiene el evento hasta que la transacción confirma;
 * si la transacción se revierte, el evento nunca se entrega.
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publicadorSpring;

    public SpringDomainEventPublisher(ApplicationEventPublisher publicadorSpring) {
        this.publicadorSpring = publicadorSpring;
    }

    @Override
    public void publicar(KudoPublicado evento) {
        publicadorSpring.publishEvent(evento);
    }
}
