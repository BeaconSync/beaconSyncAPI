package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.alerta.SituacaoNoAlerta;
import br.com.fiap.beaconsync.mensageria.EventoPublisher;
import br.com.fiap.beaconsync.mensageria.RabbitConfig;
import br.com.fiap.beaconsync.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OcorrenciaService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final PrioridadeService prioridadeService;
    private final EventoPublisher eventoPublisher;

    @Transactional
    public Ocorrencia abrirOuAtualizar(SituacaoNoAlerta situacao) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findBySituacaoId(situacao.getId()).orElse(null);

        if (ocorrencia == null) {
            ocorrencia = new Ocorrencia();
            ocorrencia.setSituacao(situacao);
            ocorrencia.setOrgao(situacao.getAlerta().getOrgao());
            ocorrencia.setStatus(StatusOcorrencia.SOLICITADA);
            ocorrenciaRepository.save(ocorrencia);
            registrarHistorico(ocorrencia, null, StatusOcorrencia.SOLICITADA, null,
                    "Pedido de ajuda registrado pelo responsável");
            eventoPublisher.publicar(RabbitConfig.RK_OCORRENCIA_STATUS, ocorrencia.getId());
        } else if (ocorrencia.getStatus() == StatusOcorrencia.CANCELADA
                || ocorrencia.getStatus() == StatusOcorrencia.CONCLUIDA) {
            mudarStatus(ocorrencia, StatusOcorrencia.SOLICITADA, null, "Novo pedido de ajuda do responsável");
        }

        ocorrencia.setScorePrioridade(prioridadeService.calcular(situacao, ocorrencia.getCriadaEm()));
        return ocorrencia;
    }

    @Transactional
    public void cancelarSeSolicitada(SituacaoNoAlerta situacao, Usuario usuario, String motivo) {
        ocorrenciaRepository.findBySituacaoId(situacao.getId())
                .filter(ocorrencia -> ocorrencia.getStatus() == StatusOcorrencia.SOLICITADA)
                .ifPresent(ocorrencia -> mudarStatus(ocorrencia, StatusOcorrencia.CANCELADA, usuario, motivo));
    }

    @Transactional
    public void mudarStatus(Ocorrencia ocorrencia, StatusOcorrencia novoStatus, Usuario usuario, String observacao) {
        StatusOcorrencia anterior = ocorrencia.getStatus();
        ocorrencia.setStatus(novoStatus);
        ocorrencia.setConcluidaEm(novoStatus == StatusOcorrencia.CONCLUIDA ? LocalDateTime.now() : null);
        registrarHistorico(ocorrencia, anterior, novoStatus, usuario, observacao);
        eventoPublisher.publicar(RabbitConfig.RK_OCORRENCIA_STATUS, ocorrencia.getId());
    }

    private void registrarHistorico(Ocorrencia ocorrencia, StatusOcorrencia anterior, StatusOcorrencia novo,
                                    Usuario usuario, String observacao) {
        HistoricoOcorrencia historico = new HistoricoOcorrencia();
        historico.setOcorrencia(ocorrencia);
        historico.setStatusAnterior(anterior);
        historico.setStatusNovo(novo);
        historico.setAlteradoPor(usuario);
        historico.setObservacao(observacao);
        ocorrencia.getHistorico().add(historico);
    }
}