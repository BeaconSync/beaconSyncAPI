package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.usuario.UsuarioResponseDTO;

public record LoginResponseDTO(
        String token,
        String tipo,
        long expiraEmSegundos,
        UsuarioResponseDTO usuario,
        String refreshToken,
        long refreshExpiraEmSegundos
) {
    @Override public String toString() { return "LoginResponseDTO[tokens=REDACTED]"; }
}