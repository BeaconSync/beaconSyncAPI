package br.com.fiap.beaconsync.mensageria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventoRabbitPublisher {

    private final AmqpTemplate amqpTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(EventoDominio evento) {
        try {
            amqpTemplate.convertAndSend(RabbitConfig.EXCHANGE, evento.routingKey(), String.valueOf(evento.id()));
            log.info("Evento publicado: {} (id {})", evento.routingKey(), evento.id());
        } catch (Exception e) {
            log.error("Falha ao publicar {} (id {}) no RabbitMQ", evento.routingKey(), evento.id(), e);
        }
    }
}