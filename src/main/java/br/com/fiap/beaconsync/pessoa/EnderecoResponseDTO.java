package br.com.fiap.beaconsync.pessoa;

public record EnderecoResponseDTO(
        Long id,
        String apelido,
        String logradouro,
        String numero,
        String complemento,
        String cep,
        Long distritoId,
        String distritoNome,
        boolean atual
) {
    public EnderecoResponseDTO(Endereco endereco, Long enderecoAtualId) {
        this(
                endereco.getId(),
                endereco.getApelido(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getCep(),
                endereco.getDistrito().getId(),
                endereco.getDistrito().getNome(),
                endereco.getId() != null && endereco.getId().equals(enderecoAtualId)
        );
    }
}