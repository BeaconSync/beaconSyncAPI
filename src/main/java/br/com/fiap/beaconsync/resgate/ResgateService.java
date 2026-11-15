package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.alerta.Situacao;
import br.com.fiap.beaconsync.alerta.SituacaoNoAlerta;
import br.com.fiap.beaconsync.auditoria.AuditoriaService;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.exception.ConflitoException;
import br.com.fiap.beaconsync.exception.RegraNegocioException;
import br.com.fiap.beaconsync.notificacao.NotificacaoService;
import br.com.fiap.beaconsync.pessoa.Endereco;
import br.com.fiap.beaconsync.pessoa.PessoaVulneravel;
import br.com.fiap.beaconsync.pessoa.StatusVinculo;
import br.com.fiap.beaconsync.pessoa.VinculoResponsavelRepository;
import br.com.fiap.beaconsync.usuario.Papel;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Operação do resgate pelo órgão: fila por prioridade, ordem manual,
 * montagem da equipe (profissionais e voluntários) e etapas do resgate.
 */
@Service
@RequiredArgsConstructor
public class ResgateService {

    private static final Set<StatusOcorrencia> ABERTAS =
            EnumSet.of(StatusOcorrencia.SOLICITADA, StatusOcorrencia.EM_ANDAMENTO);

    /** Etapas permitidas a partir de cada etapa. */
    private static final Map<StatusOcorrencia, Set<StatusOcorrencia>> TRANSICOES = Map.of(
            StatusOcorrencia.SOLICITADA, EnumSet.of(StatusOcorrencia.EM_ANDAMENTO, StatusOcorrencia.CANCELADA),
            StatusOcorrencia.EM_ANDAMENTO, EnumSet.of(StatusOcorrencia.CONCLUIDA, StatusOcorrencia.CANCELADA),
            StatusOcorrencia.CONCLUIDA, EnumSet.noneOf(StatusOcorrencia.class),
            StatusOcorrencia.CANCELADA, EnumSet.noneOf(StatusOcorrencia.class)
    );

    /** Ordem manual do agente vem primeiro (1, 2, 3...); depois o score sugerido. */
    private static final Comparator<OcorrenciaResumoDTO> ORDEM_FILA = Comparator
            .comparing((OcorrenciaResumoDTO o) -> o.prioridadeManual() == null)
            .thenComparing(o -> o.prioridadeManual() != null ? o.prioridadeManual() : 0)
            .thenComparing(OcorrenciaResumoDTO::scorePrioridade, Comparator.reverseOrder())
            .thenComparing(OcorrenciaResumoDTO::minutosEmAberto, Comparator.reverseOrder());

    private final OcorrenciaRepository ocorrenciaRepository;
    private final ResgatistaRepository resgatistaRepository;
    private final ParticipacaoResgateRepository participacaoRepository;
    private final VinculoResponsavelRepository vinculoRepository;
    private final OcorrenciaService ocorrenciaService;
    private final PrioridadeService prioridadeService;
    private final AuthorizationService authorizationService;
    private final AuditoriaService auditoriaService;
    private final NotificacaoService notificacaoService;

    /**
     * Fila do órgão do usuário logado. O score é recalculado na hora (o tempo
     * em aberto e a cor do distrito mudam), por isso a ordenação é feita aqui.
     */
    @Transactional(readOnly = true)
    public Page<OcorrenciaResumoDTO> fila(StatusOcorrencia status, Long alertaId, Pageable pageable) {
        Long orgaoId = exigirOrgao(authorizationService.usuarioLogado());
        Collection<StatusOcorrencia> etapas = status != null ? List.of(status) : ABERTAS;

        List<OcorrenciaResumoDTO> todas = ocorrenciaRepository.findByOrgaoIdAndStatusIn(orgaoId, etapas).stream()
                .filter(o -> alertaId == null || o.getSituacao().getAlerta().getId().equals(alertaId))
                .map(this::resumo)
                .sorted(ORDEM_FILA)
                .toList();

        int inicio = (int) Math.min(pageable.getOffset(), todas.size());
        int fim = Math.min(inicio + pageable.getPageSize(), todas.size());
        return new PageImpl<>(todas.subList(inicio, fim), pageable, todas.size());
    }

    /** Resgates em que o usuário logado foi escalado. */
    @Transactional(readOnly = true)
    public List<OcorrenciaResumoDTO> minhas() {
        return ocorrenciaRepository.findDoResgatista(authorizationService.usuarioLogadoId()).stream()
                .map(this::resumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public OcorrenciaDetalheDTO detalhe(Long id) {
        Ocorrencia ocorrencia = buscar(id);
        Usuario usuario = authorizationService.usuarioLogado();
        exigirAcesso(ocorrencia, usuario);

        auditoriaService.registrar(usuario.getId(), "CONSULTAR_DADOS_PESSOAIS", "OCORRENCIA", id, null);
        return detalhar(ocorrencia);
    }

    /** O sistema sugere, o agente decide: muda a ordem e a justificativa fica auditada. */
    @Transactional
    public OcorrenciaResumoDTO definirPrioridade(Long id, PrioridadeRequestDTO dto) {
        Usuario agente = authorizationService.usuarioLogado();
        Ocorrencia ocorrencia = buscar(id);
        exigirMesmoOrgao(ocorrencia, agente);

        ocorrencia.setPrioridadeManual(dto.prioridadeManual());

        auditoriaService.registrar(agente.getId(), "ALTERAR_PRIORIDADE", "OCORRENCIA", id,
                "Prioridade manual: " + dto.prioridadeManual() + ". Motivo: " + dto.justificativa());
        return resumo(ocorrencia);
    }

    /** Escala um resgatista: profissional de qualquer órgão ou voluntário disponível. */
    @Transactional
    public OcorrenciaDetalheDTO escalar(Long id, EscalaRequestDTO dto) {
        Usuario agente = authorizationService.usuarioLogado();
        Ocorrencia ocorrencia = buscar(id);
        exigirMesmoOrgao(ocorrencia, agente);

        if (!ABERTAS.contains(ocorrencia.getStatus())) {
            throw new RegraNegocioException("Só é possível montar equipe em resgates solicitados ou em andamento.");
        }
        Resgatista resgatista = resgatistaRepository.findById(dto.resgatistaId())
                .orElseThrow(() -> new EntityNotFoundException("Resgatista não encontrado."));
        if (resgatista.getTipo() == TipoResgatista.VOLUNTARIO && !Boolean.TRUE.equals(resgatista.getDisponivel())) {
            throw new RegraNegocioException("Este voluntário não está disponível no momento.");
        }
        if (participacaoRepository.existsByOcorrenciaIdAndResgatistaId(id, resgatista.getId())) {
            throw new ConflitoException("Este resgatista já está na equipe.", resgatista.getId());
        }

        ParticipacaoResgate participacao = new ParticipacaoResgate();
        participacao.setOcorrencia(ocorrencia);
        participacao.setResgatista(resgatista);
        participacao.setFuncao(dto.funcao());
        ocorrencia.getEquipe().add(participacao);
        participacaoRepository.save(participacao);

        notificacaoService.notificarEscala(resgatista.getUsuario(), ocorrencia);
        auditoriaService.registrar(agente.getId(), "ESCALAR_RESGATISTA", "OCORRENCIA", id,
                "Resgatista " + resgatista.getId() + " (" + resgatista.getTipo() + ")");
        return detalhar(ocorrencia);
    }

    /**
     * Avança a etapa do resgate. CONCLUIDA marca a pessoa como RESGATADA,
     * e o mapa recalcula a cor do distrito. A família é avisada pelo RabbitMQ.
     */
    @Transactional
    public OcorrenciaDetalheDTO atualizarStatus(Long id, StatusOcorrenciaRequestDTO dto) {
        Usuario usuario = authorizationService.usuarioLogado();
        Ocorrencia ocorrencia = buscar(id);
        exigirPodeAtualizar(ocorrencia, usuario);

        StatusOcorrencia novo = dto.status();
        if (!TRANSICOES.get(ocorrencia.getStatus()).contains(novo)) {
            throw new RegraNegocioException("Não é possível passar de %s para %s."
                    .formatted(ocorrencia.getStatus(), novo));
        }
        if (novo == StatusOcorrencia.EM_ANDAMENTO && ocorrencia.getEquipe().isEmpty()) {
            throw new RegraNegocioException("Escale ao menos um resgatista antes de iniciar o resgate.");
        }

        if (novo == StatusOcorrencia.CONCLUIDA) {
            ocorrencia.getSituacao().setSituacao(Situacao.RESGATADA);
        }
        ocorrenciaService.mudarStatus(ocorrencia, novo, usuario, dto.observacao());

        auditoriaService.registrar(usuario.getId(), "ALTERAR_ETAPA_RESGATE", "OCORRENCIA", id, novo.name());
        return detalhar(ocorrencia);
    }

    // ---- regras de acesso ----

    private Long exigirOrgao(Usuario usuario) {
        if (usuario.getOrgao() == null) {
            throw new AccessDeniedException("Seu usuário não está ligado a um órgão.");
        }
        return usuario.getOrgao().getId();
    }

    private void exigirMesmoOrgao(Ocorrencia ocorrencia, Usuario usuario) {
        if (!ocorrencia.getOrgao().getId().equals(exigirOrgao(usuario))) {
            throw new AccessDeniedException("Esta ocorrência é de outro órgão.");
        }
    }

    private boolean estaNaEquipe(Ocorrencia ocorrencia, Usuario usuario) {
        return participacaoRepository.existsByOcorrenciaIdAndResgatistaUsuarioId(ocorrencia.getId(), usuario.getId());
    }

    private boolean mesmoOrgao(Ocorrencia ocorrencia, Usuario usuario) {
        return usuario.getOrgao() != null && usuario.getOrgao().getId().equals(ocorrencia.getOrgao().getId());
    }

    /** Agente e profissional do órgão veem tudo do órgão; voluntário só o resgate em que foi escalado. */
    private void exigirAcesso(Ocorrencia ocorrencia, Usuario usuario) {
        boolean permitido = switch (usuario.getPapel()) {
            case AGENTE_ORGAO, PROFISSIONAL -> mesmoOrgao(ocorrencia, usuario) || estaNaEquipe(ocorrencia, usuario);
            case VOLUNTARIO -> estaNaEquipe(ocorrencia, usuario);
            case RESPONSAVEL -> false;
        };
        if (!permitido) {
            throw new AccessDeniedException("Você não tem acesso a esta ocorrência.");
        }
    }

    /** Muda a etapa: agente do órgão, ou profissional do órgão / escalado. */
    private void exigirPodeAtualizar(Ocorrencia ocorrencia, Usuario usuario) {
        boolean permitido = (usuario.getPapel() == Papel.AGENTE_ORGAO && mesmoOrgao(ocorrencia, usuario))
                || (usuario.getPapel() == Papel.PROFISSIONAL
                && (mesmoOrgao(ocorrencia, usuario) || estaNaEquipe(ocorrencia, usuario)));
        if (!permitido) {
            throw new AccessDeniedException("Você não pode alterar a etapa deste resgate.");
        }
    }

    // ---- montagem das respostas ----

    private OcorrenciaResumoDTO resumo(Ocorrencia ocorrencia) {
        SituacaoNoAlerta situacao = ocorrencia.getSituacao();
        PessoaVulneravel pessoa = situacao.getPessoa();
        Endereco endereco = situacao.getEndereco();

        BigDecimal score = ABERTAS.contains(ocorrencia.getStatus())
                ? prioridadeService.calcular(situacao, ocorrencia.getCriadaEm())
                : ocorrencia.getScorePrioridade();

        return new OcorrenciaResumoDTO(
                ocorrencia.getId(),
                ocorrencia.getStatus(),
                score,
                ocorrencia.getPrioridadeManual(),
                situacao.getAlerta().getId(),
                pessoa.getNome(),
                pessoa.getTipoVulnerabilidade(),
                situacao.getGravidade(),
                situacao.getRiscoImediato(),
                situacao.getOrigemTriagem(),
                situacao.getResumoEquipe(),
                endereco.getDistrito().getNome(),
                endereco.getLogradouro() + ", " + endereco.getNumero(),
                minutosEmAberto(ocorrencia),
                ocorrencia.getEquipe().size(),
                ocorrencia.getCriadaEm()
        );
    }

    private OcorrenciaDetalheDTO detalhar(Ocorrencia ocorrencia) {
        SituacaoNoAlerta situacao = ocorrencia.getSituacao();
        PessoaVulneravel pessoa = situacao.getPessoa();
        Endereco endereco = situacao.getEndereco();

        List<OcorrenciaDetalheDTO.ContatoDTO> responsaveis = vinculoRepository
                .findByPessoaIdAndStatus(pessoa.getId(), StatusVinculo.ATIVO).stream()
                .map(v -> new OcorrenciaDetalheDTO.ContatoDTO(
                        v.getUsuario().getNome(), v.getUsuario().getTelefone(), v.getParentesco()))
                .toList();

        List<OcorrenciaDetalheDTO.MembroEquipeDTO> equipe = ocorrencia.getEquipe().stream()
                .map(p -> new OcorrenciaDetalheDTO.MembroEquipeDTO(
                        p.getId(),
                        p.getResgatista().getId(),
                        p.getResgatista().getUsuario().getNome(),
                        p.getResgatista().getUsuario().getTelefone(),
                        p.getResgatista().getTipo(),
                        p.getFuncao(),
                        p.getEntrouEm()))
                .toList();

        List<OcorrenciaDetalheDTO.HistoricoDTO> historico = ocorrencia.getHistorico().stream()
                .map(h -> new OcorrenciaDetalheDTO.HistoricoDTO(
                        h.getStatusAnterior(),
                        h.getStatusNovo(),
                        h.getAlteradoPor() != null ? h.getAlteradoPor().getNome() : "sistema",
                        h.getAlteradoEm(),
                        h.getObservacao()))
                .toList();

        return new OcorrenciaDetalheDTO(
                ocorrencia.getId(),
                ocorrencia.getStatus(),
                ocorrencia.getScorePrioridade(),
                ocorrencia.getPrioridadeManual(),
                ocorrencia.getOrgao().getNome(),
                situacao.getAlerta().getId(),
                situacao.getAlerta().getTitulo(),
                pessoa.getId(),
                pessoa.getNome(),
                Period.between(pessoa.getDataNascimento(), LocalDate.now()).getYears(),
                pessoa.getTipoVulnerabilidade(),
                pessoa.getNecessidadesRemocao(),
                situacao.getSituacao(),
                situacao.getDescricaoLivre(),
                situacao.getGravidade(),
                situacao.getRiscoImediato(),
                situacao.getNecessidades(),
                situacao.getResumoEquipe(),
                situacao.getOrigemTriagem(),
                new OcorrenciaDetalheDTO.EnderecoResgateDTO(
                        endereco.getApelido(),
                        endereco.getLogradouro(),
                        endereco.getNumero(),
                        endereco.getComplemento(),
                        endereco.getCep(),
                        endereco.getDistrito().getNome()),
                responsaveis,
                equipe,
                historico,
                ocorrencia.getCriadaEm(),
                ocorrencia.getConcluidaEm()
        );
    }

    private long minutosEmAberto(Ocorrencia ocorrencia) {
        LocalDateTime fim = ocorrencia.getConcluidaEm() != null ? ocorrencia.getConcluidaEm() : LocalDateTime.now();
        return Math.max(0, Duration.between(ocorrencia.getCriadaEm(), fim).toMinutes());
    }

    private Ocorrencia buscar(Long id) {
        return ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ocorrência não encontrada."));
    }
}