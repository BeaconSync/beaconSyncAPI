package br.com.fiap.beaconsync.pessoa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VinculoRequestDTO(
        @NotBlank(message = "Informe seu parentesco com a pessoa (ex.: filha, neto, vizinho)")
        @Size(max = 40, message = "O parentesco deve ter no máximo 40 caracteres")
        String parentesco
) {}