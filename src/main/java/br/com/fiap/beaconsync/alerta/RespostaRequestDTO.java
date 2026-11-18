package br.com.fiap.beaconsync.alerta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RespostaRequestDTO(
        @NotNull(message = "Informe a pessoa (pessoaId)")
        Long pessoaId,

        @NotNull(message = "Informe a situação: SEGURA, PRECISA_AJUDA ou SEM_CONTATO")
        Situacao situacao,

        Long enderecoId,

        @Size(max = 2000, message = "O relato deve ter no máximo 2000 caracteres")
        String descricaoLivre
) {}