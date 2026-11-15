package br.com.fiap.beaconsync.resgate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisponibilidadeRequestDTO(
        @NotNull(message = "Informe disponivel: true ou false")
        Boolean disponivel,

        Long distritoBaseId,

        @Size(max = 300, message = "As habilidades devem ter no máximo 300 caracteres")
        String habilidades
) {}