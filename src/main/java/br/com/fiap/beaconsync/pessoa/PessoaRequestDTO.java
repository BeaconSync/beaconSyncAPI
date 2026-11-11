package br.com.fiap.beaconsync.pessoa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record PessoaRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotNull(message = "A data de nascimento é obrigatória")
        @PastOrPresent(message = "A data de nascimento não pode estar no futuro")
        LocalDate dataNascimento,

        @NotBlank(message = "O CPF é obrigatório")
        @CPF(message = "CPF inválido")
        String cpf,

        @NotNull(message = "Informe o tipo: ACAMADO, PESSOA_COM_DEFICIENCIA, BEBE, DEPENDENTE_APARELHO_MEDICO ou OUTRO")
        TipoVulnerabilidade tipoVulnerabilidade,

        @Size(max = 500, message = "As necessidades devem ter no máximo 500 caracteres")
        String necessidadesRemocao,

        @NotBlank(message = "Informe seu parentesco com a pessoa (ex.: filha, neto, vizinho)")
        @Size(max = 40, message = "O parentesco deve ter no máximo 40 caracteres")
        String parentesco,

        @NotNull(message = "O consentimento é obrigatório")
        @AssertTrue(message = "É preciso o consentimento da pessoa ou do responsável legal para cadastrar")
        Boolean consentimento,

        @NotNull(message = "Informe o endereço onde a pessoa está")
        @Valid
        EnderecoRequestDTO endereco
) {}