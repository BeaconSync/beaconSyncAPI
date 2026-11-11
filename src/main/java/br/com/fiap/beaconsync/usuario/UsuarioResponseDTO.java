package br.com.fiap.beaconsync.usuario;

/** Dados públicos do usuário. Nunca inclui o hash da senha. */
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String telefone,
        Papel papel,
        Long orgaoId,
        String orgaoNome
) {
    public UsuarioResponseDTO(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getPapel(),
                usuario.getOrgao() != null ? usuario.getOrgao().getId() : null,
                usuario.getOrgao() != null ? usuario.getOrgao().getNome() : null
        );
    }
}