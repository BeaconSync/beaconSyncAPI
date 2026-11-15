package br.com.fiap.beaconsync.ia;

import br.com.fiap.beaconsync.alerta.OrigemTriagem;
import br.com.fiap.beaconsync.alerta.Situacao;
import br.com.fiap.beaconsync.alerta.SituacaoNoAlerta;
import br.com.fiap.beaconsync.alerta.SituacaoNoAlertaRepository;
import br.com.fiap.beaconsync.pessoa.PessoaVulneravel;
import br.com.fiap.beaconsync.pessoa.TipoVulnerabilidade;
import br.com.fiap.beaconsync.resgate.OcorrenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Triagem do pedido de ajuda, executada pelo consumidor da fila de triagem.
 *
 * 1. Lê os dados numa transação curta.
 * 2. Chama o LLM FORA da transação (não segura conexão do banco durante a IA).
 * 3. Grava o resultado e abre a ocorrência numa segunda transação curta.
 *
 * Regra de segurança: a IA pode SUBIR a gravidade da regra base, nunca baixar.
 * Sem IA (ou se ela falhar), vale só a regra base.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriagemService {

    private final SituacaoNoAlertaRepository situacaoRepository;
    private final OcorrenciaService ocorrenciaService;
    private final TriagemIaService triagemIaService;
    private final TransactionTemplate transactionTemplate;

    public void processar(Long situacaoId) {
        DadosTriagem dados = transactionTemplate.execute(status -> carregar(situacaoId));
        if (dados == null) {
            return;
        }

        Optional<TriagemResultado> resultadoIa = triagemIaService.triar(dados);

        transactionTemplate.executeWithoutResult(status -> gravar(situacaoId, resultadoIa.orElse(null)));
    }

    private DadosTriagem carregar(Long situacaoId) {
        SituacaoNoAlerta situacao = situacaoRepository.findById(situacaoId).orElse(null);
        if (situacao == null) {
            log.warn("Situação {} não existe mais; triagem ignorada.", situacaoId);
            return null;
        }
        // A família pode ter respondido "está segura" depois do pedido
        if (!situacao.getSituacao().geraOcorrencia()) {
            log.info("Situação {} não pede mais ajuda; triagem ignorada.", situacaoId);
            return null;
        }
        // Idempotência: mensagem repetida para a mesma resposta não refaz a triagem
        if (situacao.getTriadoEm() != null && situacao.getRespondidoEm() != null
                && !situacao.getTriadoEm().isBefore(situacao.getRespondidoEm())) {
            log.info("Situação {} já triada para esta resposta.", situacaoId);
            return null;
        }

        PessoaVulneravel pessoa = situacao.getPessoa();
        return new DadosTriagem(
                pessoa.getTipoVulnerabilidade(),
                idade(pessoa),
                pessoa.getNecessidadesRemocao(),
                situacao.getSituacao(),
                Math.max(0, Duration.between(situacao.getAlerta().getEmitidoEm(), LocalDateTime.now()).toMinutes()),
                situacao.getDescricaoLivre()
        );
    }

    private void gravar(Long situacaoId, TriagemResultado resultadoIa) {
        SituacaoNoAlerta situacao = situacaoRepository.findById(situacaoId).orElse(null);
        // Pode ter mudado enquanto a IA respondia
        if (situacao == null || !situacao.getSituacao().geraOcorrencia()) {
            return;
        }

        TriagemResultado base = regraBase(situacao);
        if (resultadoIa != null) {
            aplicar(situacao, combinar(base, resultadoIa), OrigemTriagem.IA);
        } else {
            aplicar(situacao, base, OrigemTriagem.REGRA);
        }
        ocorrenciaService.abrirOuAtualizar(situacao);
        log.info("Situação {} triada por {} (gravidade {}).", situacaoId, situacao.getOrigemTriagem(),
                situacao.getGravidade());
    }

    /** A IA complementa a regra base: gravidade nunca abaixo da base; risco e necessidades somam. */
    TriagemResultado combinar(TriagemResultado base, TriagemResultado ia) {
        int gravidade = Math.max(base.gravidade(), Math.max(1, Math.min(5, ia.gravidade())));

        Set<String> necessidades = new LinkedHashSet<>();
        if (ia.necessidades() != null) {
            ia.necessidades().stream().filter(item -> item != null && !item.isBlank())
                    .map(String::trim).forEach(necessidades::add);
        }
        necessidades.addAll(base.necessidades());

        String resumo = ia.resumoParaEquipe() != null && !ia.resumoParaEquipe().isBlank()
                ? ia.resumoParaEquipe().trim()
                : base.resumoParaEquipe();

        return new TriagemResultado(gravidade, base.riscoImediato() || ia.riscoImediato(),
                new ArrayList<>(necessidades), resumo);
    }

    /** Gravidade pelo tipo de vulnerabilidade; "sem contato" soma 1 (máximo 5). */
    TriagemResultado regraBase(SituacaoNoAlerta situacao) {
        PessoaVulneravel pessoa = situacao.getPessoa();
        int gravidade = switch (pessoa.getTipoVulnerabilidade()) {
            case DEPENDENTE_APARELHO_MEDICO -> 5;
            case ACAMADO, BEBE -> 4;
            case PESSOA_COM_DEFICIENCIA -> 3;
            case OUTRO -> 2;
        };
        if (situacao.getSituacao() == Situacao.SEM_CONTATO) {
            gravidade = Math.min(gravidade + 1, 5);
        }

        List<String> necessidades = pessoa.getNecessidadesRemocao() == null ? List.of()
                : Arrays.stream(pessoa.getNecessidadesRemocao().split("[,;]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();

        String resumo = "%s, %d anos. %s.%s".formatted(
                descrever(pessoa.getTipoVulnerabilidade()),
                idade(pessoa),
                situacao.getSituacao() == Situacao.SEM_CONTATO
                        ? "Família sem contato com a pessoa"
                        : "Família pediu ajuda",
                necessidades.isEmpty() ? "" : " Levar: " + String.join(", ", necessidades) + ".");

        return new TriagemResultado(gravidade, gravidade >= 5, necessidades, resumo);
    }

    void aplicar(SituacaoNoAlerta situacao, TriagemResultado resultado, OrigemTriagem origem) {
        situacao.setGravidade(Math.max(1, Math.min(5, resultado.gravidade())));
        situacao.setRiscoImediato(resultado.riscoImediato());
        situacao.setNecessidades(limitar(String.join(", ", resultado.necessidades()), 500));
        situacao.setResumoEquipe(limitar(resultado.resumoParaEquipe(), 1000));
        situacao.setOrigemTriagem(origem);
        situacao.setTriadoEm(LocalDateTime.now());
    }

    private int idade(PessoaVulneravel pessoa) {
        return Period.between(pessoa.getDataNascimento(), LocalDate.now()).getYears();
    }

    private String descrever(TipoVulnerabilidade tipo) {
        return switch (tipo) {
            case DEPENDENTE_APARELHO_MEDICO -> "Dependente de aparelho médico";
            case ACAMADO -> "Acamado";
            case BEBE -> "Bebê";
            case PESSOA_COM_DEFICIENCIA -> "Pessoa com deficiência";
            case OUTRO -> "Pessoa vulnerável";
        };
    }

    private String limitar(String texto, int tamanho) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= tamanho ? texto : texto.substring(0, tamanho);
    }
}