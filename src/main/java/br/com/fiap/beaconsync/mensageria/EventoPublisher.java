package br.com.fiap.beaconsync.mensageria;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventoPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publicar(String routingKey, Long id) {
        applicationEventPublisher.publishEvent(new EventoDominio(routingKey, id));
    }
}