package br.com.fiap.beaconsync.pessoa;

import jakarta.validation.constraints.NotNull;

public record EnderecoAtualRequestDTO(
        @NotNull(message = "Informe o id do endereço onde a pessoa está agora")
        Long enderecoId
) {}