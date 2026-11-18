package br.com.fiap.beaconsync.usuario;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String telefone,
        Papel papel,
        Long orgaoId,
        String orgaoNome,
        String fotoUrl
) {
    public UsuarioResponseDTO(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getPapel(),
                usuario.getOrgao() != null ? usuario.getOrgao().getId() : null,
                usuario.getOrgao() != null ? usuario.getOrgao().getNome() : null,
                usuario.getFotoVersao() != null ? "/api/v1/usuarios/me/foto?v=" + usuario.getFotoVersao() : null
        );
    }
}