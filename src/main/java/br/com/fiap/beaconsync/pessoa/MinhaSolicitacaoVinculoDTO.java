package br.com.fiap.beaconsync.pessoa;

import java.time.LocalDateTime;

/** Somente o pedido do solicitante, sem dados protegidos da pessoa. */
public record MinhaSolicitacaoVinculoDTO(
        Long id,
        String parentesco,
        StatusVinculo status,
        LocalDateTime criadaEm,
        LocalDateTime decididaEm
) {}
