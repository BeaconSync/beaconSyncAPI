package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.usuario.Papel;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class TokenClaimsValidator implements OAuth2TokenValidator<Jwt> {
    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        try {
            if (token.getExpiresAt() == null || token.getIssuedAt() == null
                    || !token.getExpiresAt().isAfter(token.getIssuedAt())
                    || Long.parseLong(token.getSubject()) <= 0) {
                return invalido();
            }
            Papel.valueOf(token.getClaimAsString("role"));
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException | NullPointerException e) {
            return invalido();
        }
    }

    private OAuth2TokenValidatorResult invalido() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token", "Token sem identidade, papel ou validade válidos.", null));
    }
}