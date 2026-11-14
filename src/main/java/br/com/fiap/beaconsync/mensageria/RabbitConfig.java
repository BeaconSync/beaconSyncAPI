package br.com.fiap.beaconsync.mensageria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do RabbitMQ (opção A do requisito 5).
 *
 *   beaconsync.events (topic)
 *     resposta.registrada        -> beaconsync.triagem.q      -> TriagemListener
 *     alerta.emitido             -> beaconsync.notificacao.q  -> NotificacaoListener
 *     ocorrencia.status-alterado -> beaconsync.notificacao.q  -> NotificacaoListener
 *
 * Mensagem que falha no consumidor vai para a DLQ da fila (via beaconsync.events.dlx),
 * onde fica visível no painel do RabbitMQ para análise.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "beaconsync.events";
    public static final String DLX = "beaconsync.events.dlx";

    public static final String FILA_TRIAGEM = "beaconsync.triagem.q";
    public static final String FILA_TRIAGEM_DLQ = "beaconsync.triagem.dlq";
    public static final String FILA_NOTIFICACAO = "beaconsync.notificacao.q";
    public static final String FILA_NOTIFICACAO_DLQ = "beaconsync.notificacao.dlq";

    public static final String RK_ALERTA_EMITIDO = "alerta.emitido";
    public static final String RK_RESPOSTA_REGISTRADA = "resposta.registrada";
    public static final String RK_OCORRENCIA_STATUS = "ocorrencia.status-alterado";

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    // ---- Filas principais (duráveis, com dead letter) ----

    @Bean
    public Queue filaTriagem() {
        return QueueBuilder.durable(FILA_TRIAGEM)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(FILA_TRIAGEM_DLQ)
                .build();
    }

    @Bean
    public Queue filaNotificacao() {
        return QueueBuilder.durable(FILA_NOTIFICACAO)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(FILA_NOTIFICACAO_DLQ)
                .build();
    }

    // ---- DLQs ----

    @Bean
    public Queue filaTriagemDlq() {
        return QueueBuilder.durable(FILA_TRIAGEM_DLQ).build();
    }

    @Bean
    public Queue filaNotificacaoDlq() {
        return QueueBuilder.durable(FILA_NOTIFICACAO_DLQ).build();
    }

    // ---- Bindings ----

    @Bean
    public Binding bindingRespostaRegistrada() {
        return BindingBuilder.bind(filaTriagem()).to(eventosExchange()).with(RK_RESPOSTA_REGISTRADA);
    }

    @Bean
    public Binding bindingAlertaEmitido() {
        return BindingBuilder.bind(filaNotificacao()).to(eventosExchange()).with(RK_ALERTA_EMITIDO);
    }

    @Bean
    public Binding bindingOcorrenciaStatus() {
        return BindingBuilder.bind(filaNotificacao()).to(eventosExchange()).with(RK_OCORRENCIA_STATUS);
    }

    @Bean
    public Binding bindingTriagemDlq() {
        return BindingBuilder.bind(filaTriagemDlq()).to(deadLetterExchange()).with(FILA_TRIAGEM_DLQ);
    }

    @Bean
    public Binding bindingNotificacaoDlq() {
        return BindingBuilder.bind(filaNotificacaoDlq()).to(deadLetterExchange()).with(FILA_NOTIFICACAO_DLQ);
    }
}