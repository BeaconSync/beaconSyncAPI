package br.com.fiap.beaconsync.ia;

import br.com.fiap.beaconsync.alerta.Situacao;
import br.com.fiap.beaconsync.pessoa.TipoVulnerabilidade;

public record DadosTriagem(
        TipoVulnerabilidade tipoVulnerabilidade,
        int idade,
        String necessidadesCadastro,
        Situacao situacao,
        long minutosDesdeAlerta,
        String relatoResponsavel
) {}