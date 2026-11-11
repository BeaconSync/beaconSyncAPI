package br.com.fiap.beaconsync.pessoa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PessoaUpdateDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotNull(message = "A data de nascimento é obrigatória")
        @PastOrPresent(message = "A data de nascimento não pode estar no futuro")
        LocalDate dataNascimento,

        @NotNull(message = "O tipo de vulnerabilidade é obrigatório")
        TipoVulnerabilidade tipoVulnerabilidade,

        @Size(max = 500, message = "As necessidades devem ter no máximo 500 caracteres")
        String necessidadesRemocao
) {}