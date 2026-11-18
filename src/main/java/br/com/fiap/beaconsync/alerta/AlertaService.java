package br.com.fiap.beaconsync.alerta;

import br.com.fiap.beaconsync.auditoria.AuditoriaService;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.distrito.Distrito;
import br.com.fiap.beaconsync.distrito.DistritoRepository;
import br.com.fiap.beaconsync.exception.RegraNegocioException;
import br.com.fiap.beaconsync.mensageria.EventoPublisher;
import br.com.fiap.beaconsync.mensageria.RabbitConfig;
import br.com.fiap.beaconsync.pessoa.Endereco;
import br.com.fiap.beaconsync.pessoa.EnderecoRepository;
import br.com.fiap.beaconsync.pessoa.PessoaVulneravel;
import br.com.fiap.beaconsync.pessoa.PessoaVulneravelRepository;
import br.com.fiap.beaconsync.pessoa.StatusVinculo;
import br.com.fiap.beaconsync.pessoa.VinculoResponsavelRepository;
import br.com.fiap.beaconsync.resgate.Ocorrencia;
import br.com.fiap.beaconsync.resgate.OcorrenciaRepository;
import br.com.fiap.beaconsync.resgate.OcorrenciaService;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final SituacaoNoAlertaRepository situacaoRepository;
    private final DistritoRepository distritoRepository;
    private final PessoaVulneravelRepository pessoaRepository;
    private final EnderecoRepository enderecoRepository;
    private final VinculoResponsavelRepository vinculoRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final OcorrenciaService ocorrenciaService;
    private final AuthorizationService authorizationService;
    private final AuditoriaService auditoriaService;
    private final EventoPublisher eventoPublisher;

    @Transactional
    public AlertaResponseDTO emitir(AlertaRequestDTO dto) {
        Usuario agente = authorizationService.usuarioLogado();
        if (agente.getOrgao() == null) {
            throw new RegraNegocioException("Seu usuário não está ligado a um órgão.");
        }

        Set<Long> distritoIds = new HashSet<>(dto.distritoIds());
        List<Distrito> distritos = distritoRepository.findAllById(distritoIds);
        if (distritos.size() != distritoIds.size()) {
            throw new RegraNegocioException("Um ou mais distritos informados não existem.");
        }

        Alerta alerta = new Alerta();
        alerta.setOrgao(agente.getOrgao());
        alerta.setEmitidoPor(agente);
        alerta.setTitulo(dto.titulo().trim());
        alerta.setDescricao(dto.descricao());
        alerta.getDistritos().addAll(distritos);
        alertaRepository.save(alerta);

        List<SituacaoNoAlerta> situacoes = pessoaRepository.findComEnderecoAtualNosDistritos(distritoIds).stream()
                .map(pessoa -> novaSituacao(alerta, pessoa))
                .toList();
        situacaoRepository.saveAll(situacoes);

        auditoriaService.registrar(agente.getId(), "EMITIR_ALERTA", "ALERTA", alerta.getId(),
                distritoIds.size() + " distritos, " + situacoes.size() + " pessoas afetadas");
        eventoPublisher.publicar(RabbitConfig.RK_ALERTA_EMITIDO, alerta.getId());

        return new AlertaResponseDTO(alerta, situacoes.size());
    }

    @Transactional(readOnly = true)
    public Page<AlertaResponseDTO> listar(StatusAlerta status, Pageable pageable) {
        Page<Alerta> alertas = status != null
                ? alertaRepository.findByStatus(status, pageable)
                : alertaRepository.findAll(pageable);
        return alertas.map(alerta -> new AlertaResponseDTO(alerta, situacaoRepository.countByAlertaId(alerta.getId())));
    }

    @Transactional(readOnly = true)
    public AlertaResponseDTO buscarPorId(Long id) {
        Alerta alerta = buscar(id);
        return new AlertaResponseDTO(alerta, situacaoRepository.countByAlertaId(id));
    }

    @Transactional
    public AlertaResponseDTO encerrar(Long id) {
        Usuario agente = authorizationService.usuarioLogado();
        Alerta alerta = buscar(id);
        if (agente.getOrgao() == null || !agente.getOrgao().getId().equals(alerta.getOrgao().getId())) {
            throw new AccessDeniedException("Só o órgão que emitiu o alerta pode encerrá-lo.");
        }
        if (alerta.getStatus() == StatusAlerta.ENCERRADO) {
            throw new RegraNegocioException("Este alerta já está encerrado.");
        }
        alerta.setStatus(StatusAlerta.ENCERRADO);
        alerta.setEncerradoEm(LocalDateTime.now());

        auditoriaService.registrar(agente.getId(), "ENCERRAR_ALERTA", "ALERTA", id, null);
        return new AlertaResponseDTO(alerta, situacaoRepository.countByAlertaId(id));
    }

    @Transactional(readOnly = true)
    public List<SituacaoResponseDTO> minhasSituacoesAtivas() {
        return situacaoRepository
                .findAtivasDoResponsavel(authorizationService.usuarioLogadoId(), StatusAlerta.ATIVO, StatusVinculo.ATIVO)
                .stream()
                .map(SituacaoResponseDTO::new)
                .toList();
    }

    @Transactional
    public SituacaoResponseDTO responder(Long alertaId, RespostaRequestDTO dto) {
        if (dto.situacao() == Situacao.PENDENTE || dto.situacao() == Situacao.RESGATADA) {
            throw new RegraNegocioException("Responda com SEGURA, PRECISA_AJUDA ou SEM_CONTATO.");
        }
        authorizationService.exigirVinculoAtivo(dto.pessoaId());
        Usuario responsavel = authorizationService.usuarioLogado();

        Alerta alerta = buscar(alertaId);
        if (alerta.getStatus() != StatusAlerta.ATIVO) {
            throw new RegraNegocioException("Este alerta já foi encerrado.");
        }
        PessoaVulneravel pessoa = pessoaRepository.findById(dto.pessoaId())
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));

        if (dto.enderecoId() != null && !dto.enderecoId().equals(idEnderecoAtual(pessoa))) {
            Endereco novoEndereco = enderecoRepository.findById(dto.enderecoId())
                    .filter(endereco -> endereco.getPessoa().getId().equals(pessoa.getId()))
                    .orElseThrow(() -> new RegraNegocioException("O endereço informado não pertence a esta pessoa."));
            pessoa.setEnderecoAtual(novoEndereco);
            sincronizarEnderecoAtual(pessoa);
        }

        SituacaoNoAlerta situacao = situacaoRepository.findByAlertaIdAndPessoaId(alertaId, pessoa.getId())
                .orElseThrow(() -> new RegraNegocioException("Esta pessoa não está na área deste alerta."));
        if (situacao.getSituacao() == Situacao.RESGATADA) {
            throw new RegraNegocioException("Esta pessoa já foi resgatada neste alerta.");
        }

        situacao.setSituacao(dto.situacao());
        situacao.setDescricaoLivre(dto.descricaoLivre());
        situacao.setRespondidoPor(responsavel);
        situacao.setRespondidoEm(LocalDateTime.now());

        if (dto.situacao().geraOcorrencia()) {
            eventoPublisher.publicar(RabbitConfig.RK_RESPOSTA_REGISTRADA, situacao.getId());
        } else {
            ocorrenciaService.cancelarSeSolicitada(situacao, responsavel,
                    "Responsável informou que a pessoa está segura");
        }

        auditoriaService.registrar(responsavel.getId(), "RESPONDER_ALERTA", "SITUACAO_ALERTA", situacao.getId(),
                "Alerta " + alertaId + ": " + dto.situacao());
        return new SituacaoResponseDTO(situacao);
    }

    @Transactional(readOnly = true)
    public List<PessoaNoDistritoDTO> pessoasDoDistrito(Long alertaId, Long distritoId) {
        Alerta alerta = buscar(alertaId);
        if (alerta.getStatus() != StatusAlerta.ATIVO) {
            throw new RegraNegocioException("Os dados pessoais só ficam disponíveis durante um alerta ativo.");
        }
        boolean distritoNoAlerta = alerta.getDistritos().stream().anyMatch(d -> d.getId().equals(distritoId));
        if (!distritoNoAlerta) {
            throw new RegraNegocioException("Este distrito não faz parte do alerta.");
        }

        List<PessoaNoDistritoDTO> lista = situacaoRepository.findByAlertaIdAndEnderecoDistritoId(alertaId, distritoId)
                .stream()
                .map(this::paraPessoaNoDistrito)
                .sorted(PRIORIDADE)
                .toList();

        auditoriaService.registrar(authorizationService.usuarioLogadoId(), "CONSULTAR_DADOS_PESSOAIS", "ALERTA",
                alertaId, "Distrito " + distritoId + ": " + lista.size() + " pessoas");
        return lista;
    }

    @Transactional
    public void sincronizarEnderecoAtual(PessoaVulneravel pessoa) {
        Endereco atual = pessoa.getEnderecoAtual();
        if (atual == null) {
            return;
        }
        for (Alerta alerta : alertaRepository.findByStatus(StatusAlerta.ATIVO)) {
            Optional<SituacaoNoAlerta> existente = situacaoRepository.findByAlertaIdAndPessoaId(alerta.getId(), pessoa.getId());
            if (existente.isPresent()) {
                existente.get().setEndereco(atual);
            } else if (alerta.getDistritos().stream().anyMatch(d -> d.getId().equals(atual.getDistrito().getId()))) {
                situacaoRepository.save(novaSituacao(alerta, pessoa));
            }
        }
    }

    private static final Comparator<PessoaNoDistritoDTO> PRIORIDADE = Comparator
            .comparingInt((PessoaNoDistritoDTO p) -> ordemSituacao(p.situacao()))
            .thenComparing(p -> p.gravidade() != null ? p.gravidade() : 0, Comparator.reverseOrder())
            .thenComparing(p -> Boolean.TRUE.equals(p.riscoImediato()), Comparator.reverseOrder())
            .thenComparing(PessoaNoDistritoDTO::minutosEmAberto, Comparator.reverseOrder());

    private static int ordemSituacao(Situacao situacao) {
        return switch (situacao) {
            case PRECISA_AJUDA, SEM_CONTATO -> 0;
            case PENDENTE -> 1;
            case SEGURA -> 2;
            case RESGATADA -> 3;
        };
    }

    private PessoaNoDistritoDTO paraPessoaNoDistrito(SituacaoNoAlerta situacao) {
        PessoaVulneravel pessoa = situacao.getPessoa();
        Endereco endereco = situacao.getEndereco();
        LocalDateTime desde = situacao.getRespondidoEm() != null
                ? situacao.getRespondidoEm()
                : situacao.getAlerta().getEmitidoEm();
        Optional<Ocorrencia> ocorrencia = ocorrenciaRepository.findBySituacaoId(situacao.getId());

        List<PessoaNoDistritoDTO.ContatoResponsavelDTO> responsaveis = vinculoRepository
                .findByPessoaIdAndStatus(pessoa.getId(), StatusVinculo.ATIVO).stream()
                .map(v -> new PessoaNoDistritoDTO.ContatoResponsavelDTO(
                        v.getUsuario().getNome(), v.getUsuario().getTelefone(), v.getParentesco()))
                .toList();

        return new PessoaNoDistritoDTO(
                situacao.getId(),
                pessoa.getId(),
                pessoa.getNome(),
                Period.between(pessoa.getDataNascimento(), LocalDate.now()).getYears(),
                pessoa.getTipoVulnerabilidade(),
                pessoa.getNecessidadesRemocao(),
                endereco.getApelido(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getCep(),
                situacao.getSituacao(),
                situacao.getGravidade(),
                situacao.getRiscoImediato(),
                situacao.getResumoEquipe(),
                Math.max(0, Duration.between(desde, LocalDateTime.now()).toMinutes()),
                ocorrencia.map(Ocorrencia::getId).orElse(null),
                ocorrencia.map(Ocorrencia::getStatus).orElse(null),
                responsaveis
        );
    }

    private SituacaoNoAlerta novaSituacao(Alerta alerta, PessoaVulneravel pessoa) {
        SituacaoNoAlerta situacao = new SituacaoNoAlerta();
        situacao.setAlerta(alerta);
        situacao.setPessoa(pessoa);
        situacao.setEndereco(pessoa.getEnderecoAtual());
        situacao.setSituacao(Situacao.PENDENTE);
        return situacao;
    }

    private Long idEnderecoAtual(PessoaVulneravel pessoa) {
        return pessoa.getEnderecoAtual() != null ? pessoa.getEnderecoAtual().getId() : null;
    }

    private Alerta buscar(Long id) {
        return alertaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alerta não encontrado."));
    }
}