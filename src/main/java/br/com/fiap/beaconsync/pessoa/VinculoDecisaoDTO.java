package br.com.fiap.beaconsync.pessoa;

import jakarta.validation.constraints.NotNull;

public record VinculoDecisaoDTO(
        @NotNull(message = "Informe aprovar: true ou false")
        Boolean aprovar
) {}