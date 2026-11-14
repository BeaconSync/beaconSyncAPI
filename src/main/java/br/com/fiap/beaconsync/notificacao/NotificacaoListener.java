package br.com.fiap.beaconsync.mensageria;

import br.com.fiap.beaconsync.notificacao.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/** Consome alerta.emitido e ocorrencia.status-alterado e grava os avisos lidos pelo app. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacaoListener {

    private final NotificacaoService notificacaoService;

    @RabbitListener(queues = RabbitConfig.FILA_NOTIFICACAO)
    public void receber(String id, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        log.info("Notificação recebida: {} (id {})", routingKey, id);
        switch (routingKey) {
            case RabbitConfig.RK_ALERTA_EMITIDO -> notificacaoService.notificarAlertaEmitido(Long.valueOf(id));
            case RabbitConfig.RK_OCORRENCIA_STATUS -> notificacaoService.notificarMudancaOcorrencia(Long.valueOf(id));
            default -> log.warn("Routing key sem tratamento: {}", routingKey);
        }
    }
}