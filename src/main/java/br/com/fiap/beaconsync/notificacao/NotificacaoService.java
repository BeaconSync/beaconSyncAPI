package br.com.fiap.beaconsync.notificacao;

import br.com.fiap.beaconsync.alerta.Alerta;
import br.com.fiap.beaconsync.alerta.AlertaRepository;
import br.com.fiap.beaconsync.alerta.SituacaoNoAlerta;
import br.com.fiap.beaconsync.alerta.SituacaoNoAlertaRepository;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.pessoa.PessoaVulneravel;
import br.com.fiap.beaconsync.pessoa.StatusVinculo;
import br.com.fiap.beaconsync.pessoa.VinculoResponsavel;
import br.com.fiap.beaconsync.pessoa.VinculoResponsavelRepository;
import br.com.fiap.beaconsync.resgate.Ocorrencia;
import br.com.fiap.beaconsync.resgate.OcorrenciaRepository;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final AlertaRepository alertaRepository;
    private final SituacaoNoAlertaRepository situacaoRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final VinculoResponsavelRepository vinculoRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public void notificarAlertaEmitido(Long alertaId) {
        Alerta alerta = alertaRepository.findById(alertaId).orElse(null);
        if (alerta == null) {
            log.warn("Alerta {} não encontrado para notificação.", alertaId);
            return;
        }
        int total = 0;
        for (SituacaoNoAlerta situacao : situacaoRepository.findByAlertaId(alertaId)) {
            PessoaVulneravel pessoa = situacao.getPessoa();
            String mensagem = "Confirme onde %s está e se está segura. Distrito: %s."
                    .formatted(pessoa.getNome(), situacao.getEndereco().getDistrito().getNome());
            total += avisarResponsaveis(pessoa, "Alerta de enchente: " + alerta.getTitulo(), mensagem);
        }
        log.info("Alerta {}: {} avisos gravados.", alertaId, total);
    }

    @Transactional
    public void notificarMudancaOcorrencia(Long ocorrenciaId) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(ocorrenciaId).orElse(null);
        if (ocorrencia == null) {
            log.warn("Ocorrência {} não encontrada para notificação.", ocorrenciaId);
            return;
        }
        PessoaVulneravel pessoa = ocorrencia.getSituacao().getPessoa();
        String nome = pessoa.getNome();
        String mensagem = switch (ocorrencia.getStatus()) {
            case SOLICITADA -> "O pedido de resgate de %s foi registrado. O órgão %s vai organizar a equipe."
                    .formatted(nome, ocorrencia.getOrgao().getNome());
            case EM_ANDAMENTO -> "A equipe de resgate está a caminho ou no local onde %s está.".formatted(nome);
            case CONCLUIDA -> "%s foi retirada do local e está em segurança.".formatted(nome);
            case CANCELADA -> "O pedido de resgate de %s foi cancelado.".formatted(nome);
        };
        avisarResponsaveis(pessoa, "Resgate de " + nome, mensagem);
    }

    @Transactional
    public void notificarEscala(Usuario resgatista, Ocorrencia ocorrencia) {
        criar(resgatista, "Você foi escalado para um resgate",
                "Resgate %d no distrito %s. Abra os detalhes no app para ver endereço e necessidades."
                        .formatted(ocorrencia.getId(), ocorrencia.getSituacao().getEndereco().getDistrito().getNome()));
    }

    @Transactional(readOnly = true)
    public Page<NotificacaoResponseDTO> listarMinhas(Pageable pageable) {
        return notificacaoRepository.findByUsuarioId(authorizationService.usuarioLogadoId(), pageable)
                .map(NotificacaoResponseDTO::new);
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoLida(Long id) {
        Notificacao notificacao = notificacaoRepository.findByIdAndUsuarioId(id, authorizationService.usuarioLogadoId())
                .orElseThrow(() -> new EntityNotFoundException("Notificação não encontrada."));
        notificacao.setLida(Boolean.TRUE);
        return new NotificacaoResponseDTO(notificacao);
    }

    private int avisarResponsaveis(PessoaVulneravel pessoa, String titulo, String mensagem) {
        int total = 0;
        for (VinculoResponsavel vinculo : vinculoRepository.findByPessoaIdAndStatus(pessoa.getId(), StatusVinculo.ATIVO)) {
            criar(vinculo.getUsuario(), titulo, mensagem);
            total++;
        }
        return total;
    }

    private void criar(Usuario usuario, String titulo, String mensagem) {
        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(usuario);
        notificacao.setTitulo(titulo.length() <= 120 ? titulo : titulo.substring(0, 120));
        notificacao.setMensagem(mensagem.length() <= 500 ? mensagem : mensagem.substring(0, 500));
        notificacaoRepository.save(notificacao);
    }
}