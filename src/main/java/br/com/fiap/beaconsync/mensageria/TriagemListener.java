package br.com.fiap.beaconsync.mensageria;

import br.com.fiap.beaconsync.ia.TriagemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consome resposta.registrada: faz a triagem e abre (ou atualiza) a ocorrência. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriagemListener {

    private final TriagemService triagemService;

    @RabbitListener(queues = RabbitConfig.FILA_TRIAGEM)
    public void aoRegistrarResposta(String situacaoId) {
        log.info("Triagem recebida para a situação {}", situacaoId);
        triagemService.processar(Long.valueOf(situacaoId));
    }
}