package br.com.fiap.beaconsync.auth;

import org.springframework.security.authentication.BadCredentialsException;

public class RefreshTokenInvalidoException extends BadCredentialsException {
    public RefreshTokenInvalidoException() {
        super("Sessão de renovação inválida ou expirada. Faça login novamente.");
    }
}