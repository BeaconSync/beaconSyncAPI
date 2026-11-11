package br.com.fiap.beaconsync.pessoa;

import java.time.LocalDateTime;

public record VinculoResponseDTO(
        Long id,
        Long pessoaId,
        Long usuarioId,
        String usuarioNome,
        String usuarioTelefone,
        String parentesco,
        StatusVinculo status,
        LocalDateTime criadoEm
) {
    public VinculoResponseDTO(VinculoResponsavel vinculo) {
        this(
                vinculo.getId(),
                vinculo.getPessoa().getId(),
                vinculo.getUsuario().getId(),
                vinculo.getUsuario().getNome(),
                vinculo.getUsuario().getTelefone(),
                vinculo.getParentesco(),
                vinculo.getStatus(),
                vinculo.getCriadoEm()
        );
    }
}