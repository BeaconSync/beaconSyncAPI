package br.com.fiap.beaconsync.resgate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusOcorrenciaRequestDTO(
        @NotNull(message = "Informe a nova etapa: EM_ANDAMENTO, CONCLUIDA ou CANCELADA")
        StatusOcorrencia status,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao
) {}