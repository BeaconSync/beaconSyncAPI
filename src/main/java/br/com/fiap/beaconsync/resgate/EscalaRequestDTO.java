package br.com.fiap.beaconsync.resgate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EscalaRequestDTO(
        @NotNull(message = "Informe o resgatista (resgatistaId de GET /api/v1/resgatistas/disponiveis)")
        Long resgatistaId,

        @Size(max = 60, message = "A função deve ter no máximo 60 caracteres")
        String funcao
) {}