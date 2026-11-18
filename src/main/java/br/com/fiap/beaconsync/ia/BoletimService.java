package br.com.fiap.beaconsync.ia;

import br.com.fiap.beaconsync.alerta.ContagemPorDistrito;
import br.com.fiap.beaconsync.alerta.MapaDistritoDTO;
import br.com.fiap.beaconsync.alerta.MapaResponseDTO;
import br.com.fiap.beaconsync.alerta.MapaService;
import br.com.fiap.beaconsync.alerta.Situacao;
import br.com.fiap.beaconsync.alerta.SituacaoNoAlertaRepository;
import br.com.fiap.beaconsync.pessoa.TipoVulnerabilidade;
import br.com.fiap.beaconsync.resgate.ContagemOcorrencia;
import br.com.fiap.beaconsync.resgate.OcorrenciaRepository;
import br.com.fiap.beaconsync.resgate.StatusOcorrencia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BoletimService {

    private static final Set<Situacao> SEM_SEGURANCA =
            EnumSet.of(Situacao.PENDENTE, Situacao.PRECISA_AJUDA, Situacao.SEM_CONTATO);
    private static final Set<TipoVulnerabilidade> TIPOS_CRITICOS =
            EnumSet.of(TipoVulnerabilidade.ACAMADO, TipoVulnerabilidade.DEPENDENTE_APARELHO_MEDICO);

    private static final String PROMPT_SISTEMA = """
            Você escreve boletins curtos para o agente da Defesa Civil durante uma enchente.
            Use somente os números fornecidos; não invente dados, nomes nem endereços.
            Comece pelos distritos mais críticos (vermelho e laranja) e pelo que exige ação agora.
            No máximo 6 linhas, em português, frases curtas, sem markdown.
            """;

    private final MapaService mapaService;
    private final SituacaoNoAlertaRepository situacaoRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final IaDisponibilidade iaDisponibilidade;
    private final ChatClient chatClient;

    public BoletimService(MapaService mapaService, SituacaoNoAlertaRepository situacaoRepository,
                          OcorrenciaRepository ocorrenciaRepository, IaDisponibilidade iaDisponibilidade,
                          ChatClient.Builder builder) {
        this.mapaService = mapaService;
        this.situacaoRepository = situacaoRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.iaDisponibilidade = iaDisponibilidade;
        this.chatClient = builder.defaultSystem(PROMPT_SISTEMA).build();
    }

    public BoletimResponseDTO gerar(Long alertaId) {
        MapaResponseDTO mapa = mapaService.gerar(alertaId);

        Map<Long, Long> criticos = situacaoRepository
                .contarPorTipoEDistrito(alertaId, SEM_SEGURANCA, TIPOS_CRITICOS).stream()
                .collect(Collectors.toMap(ContagemPorDistrito::getDistritoId, ContagemPorDistrito::getTotal));

        Map<Long, Map<StatusOcorrencia, Long>> ocorrencias = new HashMap<>();
        for (ContagemOcorrencia linha : ocorrenciaRepository.contarPorDistrito(alertaId)) {
            ocorrencias.computeIfAbsent(linha.getDistritoId(), id -> new HashMap<>())
                    .put(linha.getStatus(), linha.getTotal());
        }

        List<BoletimDistritoDTO> distritos = mapa.distritos().stream()
                .map(d -> paraBoletim(d, criticos, ocorrencias))
                .toList();

        String texto = null;
        String origem = "MODELO";
        if (iaDisponibilidade.habilitada()) {
            try {
                texto = chatClient.prompt()
                        .user(numerosParaIa(mapa, distritos))
                        .call()
                        .content();
                origem = "IA";
            } catch (Exception e) {
                log.warn("IA indisponível no boletim; usando o modelo de texto. Motivo: {}", e.getMessage());
            }
        }
        if (texto == null || texto.isBlank()) {
            texto = textoModelo(mapa, distritos);
            origem = "MODELO";
        }

        return new BoletimResponseDTO(alertaId, mapa.titulo(), LocalDateTime.now(), origem, texto.trim(), distritos);
    }

    private BoletimDistritoDTO paraBoletim(MapaDistritoDTO d, Map<Long, Long> criticos,
                                           Map<Long, Map<StatusOcorrencia, Long>> ocorrencias) {
        Map<StatusOcorrencia, Long> porStatus = ocorrencias.getOrDefault(d.distritoId(), Map.of());
        return new BoletimDistritoDTO(
                d.distritoId(),
                d.nome(),
                d.cor(),
                d.total(),
                d.pendentes(),
                d.precisamAjuda(),
                d.semContato(),
                criticos.getOrDefault(d.distritoId(), 0L),
                porStatus.getOrDefault(StatusOcorrencia.EM_ANDAMENTO, 0L),
                porStatus.getOrDefault(StatusOcorrencia.CONCLUIDA, 0L)
        );
    }

    private String numerosParaIa(MapaResponseDTO mapa, List<BoletimDistritoDTO> distritos) {
        StringBuilder texto = new StringBuilder()
                .append("Alerta: ").append(mapa.titulo().replace('{', '(').replace('}', ')')).append('\n')
                .append("Pessoas ainda sem segurança no total: ").append(mapa.totalSemSeguranca()).append('\n')
                .append("Por distrito:\n");
        for (BoletimDistritoDTO d : distritos) {
            texto.append("- %s (cor %s): %d sem segurança, %d sem resposta, %d pedem ajuda, %d sem contato, "
                            .formatted(d.nome(), d.cor(), d.semSeguranca(), d.semResposta(), d.precisamAjuda(), d.semContato()))
                    .append("%d acamados ou dependentes de aparelho, %d resgates em andamento, %d resgates concluídos\n"
                            .formatted(d.acamadosOuDependentes(), d.resgatesEmAndamento(), d.resgatesConcluidos()));
        }
        return texto.toString();
    }

    private String textoModelo(MapaResponseDTO mapa, List<BoletimDistritoDTO> distritos) {
        StringBuilder texto = new StringBuilder("Boletim do alerta \"%s\": %d pessoas ainda sem segurança."
                .formatted(mapa.titulo(), mapa.totalSemSeguranca()));
        distritos.stream()
                .filter(d -> d.semSeguranca() > 0 || d.resgatesEmAndamento() > 0)
                .forEach(d -> texto.append("\nDistrito %s (%s): %d sem resposta, %d pedem ajuda, %d sem contato, "
                                .formatted(d.nome(), d.cor(), d.semResposta(), d.precisamAjuda(), d.semContato()))
                        .append("%d acamados ou dependentes de aparelho, %d resgates em andamento."
                                .formatted(d.acamadosOuDependentes(), d.resgatesEmAndamento())));
        return texto.toString();
    }
}