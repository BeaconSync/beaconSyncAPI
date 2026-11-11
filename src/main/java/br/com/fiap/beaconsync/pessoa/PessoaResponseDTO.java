package br.com.fiap.beaconsync.pessoa;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PessoaResponseDTO(
        Long id,
        String nome,
        LocalDate dataNascimento,
        String cpfMascarado,
        TipoVulnerabilidade tipoVulnerabilidade,
        String necessidadesRemocao,
        LocalDateTime consentimentoEm,
        EnderecoResponseDTO enderecoAtual,
        List<EnderecoResponseDTO> enderecos
) {
    public PessoaResponseDTO(PessoaVulneravel pessoa) {
        this(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getDataNascimento(),
                mascararCpf(pessoa.getCpf()),
                pessoa.getTipoVulnerabilidade(),
                pessoa.getNecessidadesRemocao(),
                pessoa.getConsentimentoEm(),
                pessoa.getEnderecoAtual() != null
                        ? new EnderecoResponseDTO(pessoa.getEnderecoAtual(), pessoa.getEnderecoAtual().getId())
                        : null,
                pessoa.getEnderecos().stream()
                        .map(endereco -> new EnderecoResponseDTO(endereco, idEnderecoAtual(pessoa)))
                        .toList()
        );
    }

    private static Long idEnderecoAtual(PessoaVulneravel pessoa) {
        return pessoa.getEnderecoAtual() != null ? pessoa.getEnderecoAtual().getId() : null;
    }

    private static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return null;
        }
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}