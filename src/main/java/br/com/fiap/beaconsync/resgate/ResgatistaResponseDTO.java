package br.com.fiap.beaconsync.resgate;

public record ResgatistaResponseDTO(
        Long id,
        Long usuarioId,
        String nome,
        String telefone,
        TipoResgatista tipo,
        String habilidades,
        boolean disponivel,
        String orgaoNome,
        Long distritoBaseId,
        String distritoBaseNome
) {
    public ResgatistaResponseDTO(Resgatista resgatista) {
        this(
                resgatista.getId(),
                resgatista.getUsuario().getId(),
                resgatista.getUsuario().getNome(),
                resgatista.getUsuario().getTelefone(),
                resgatista.getTipo(),
                resgatista.getHabilidades(),
                Boolean.TRUE.equals(resgatista.getDisponivel()),
                resgatista.getOrgao() != null ? resgatista.getOrgao().getNome() : null,
                resgatista.getDistritoBase() != null ? resgatista.getDistritoBase().getId() : null,
                resgatista.getDistritoBase() != null ? resgatista.getDistritoBase().getNome() : null
        );
    }
}