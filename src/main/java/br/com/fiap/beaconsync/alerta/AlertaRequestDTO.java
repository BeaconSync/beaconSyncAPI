package br.com.fiap.beaconsync.alerta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Alerta emitido por um agente do órgão para uma lista de distritos. */
public record AlertaRequestDTO(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 120, message = "O título deve ter no máximo 120 caracteres")
        String titulo,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao,

        @NotEmpty(message = "Informe ao menos um distrito (ids de GET /api/v1/distritos)")
        @Size(max = 96, message = "São no máximo 96 distritos")
        List<@NotNull(message = "Id de distrito não pode ser nulo") Long> distritoIds
) {}