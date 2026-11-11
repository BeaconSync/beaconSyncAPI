package br.com.fiap.beaconsync.pessoa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequestDTO(
        @NotBlank(message = "O apelido do endereço é obrigatório (ex.: Casa, Casa da filha, Centro-dia)")
        @Size(max = 60, message = "O apelido deve ter no máximo 60 caracteres")
        String apelido,

        @NotBlank(message = "O logradouro é obrigatório")
        @Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres")
        String logradouro,

        @NotBlank(message = "O número é obrigatório (use S/N se não houver)")
        @Size(max = 10, message = "O número deve ter no máximo 10 caracteres")
        String numero,

        @Size(max = 60, message = "O complemento deve ter no máximo 60 caracteres")
        String complemento,

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido. Use o formato 00000-000")
        String cep,

        @NotNull(message = "Informe o distrito (distritoId de GET /api/v1/distritos)")
        Long distritoId
) {}