package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.alerta.OrigemTriagem;
import br.com.fiap.beaconsync.pessoa.TipoVulnerabilidade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OcorrenciaResumoDTO(
        Long id,
        StatusOcorrencia status,
        BigDecimal scorePrioridade,
        Integer prioridadeManual,
        Long alertaId,
        String pessoaNome,
        TipoVulnerabilidade tipoVulnerabilidade,
        Integer gravidade,
        Boolean riscoImediato,
        OrigemTriagem origemTriagem,
        String resumoEquipe,
        String distritoNome,
        String endereco,
        long minutosEmAberto,
        int tamanhoEquipe,
        LocalDateTime criadaEm
) {}