package br.com.fiap.beaconsync.ia;

import br.com.fiap.beaconsync.alerta.Situacao;
import br.com.fiap.beaconsync.pessoa.TipoVulnerabilidade;

/**
 * O que vai para o LLM na triagem. De propósito, sem nome, CPF ou endereço:
 * a IA só precisa saber o tipo de vulnerabilidade e o que a família relatou.
 */
public record DadosTriagem(
        TipoVulnerabilidade tipoVulnerabilidade,
        int idade,
        String necessidadesCadastro,
        Situacao situacao,
        long minutosDesdeAlerta,
        String relatoResponsavel
) {}