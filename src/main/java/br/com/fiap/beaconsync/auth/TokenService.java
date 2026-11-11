package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.usuario.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long expiracaoSegundos;
    private final String issuer;

    public TokenService(JwtEncoder encoder,
                        @Value("${beaconsync.jwt.expiracao-segundos}") long expiracaoSegundos,
                        @Value("${beaconsync.jwt.issuer}") String issuer) {
        if (expiracaoSegundos < 60 || expiracaoSegundos > 86_400) {
            throw new IllegalArgumentException("A validade do JWT deve ficar entre 60 segundos e 24 horas.");
        }
        this.encoder = encoder;
        this.expiracaoSegundos = expiracaoSegundos;
        this.issuer = issuer;
    }

    public long getExpiracaoSegundos() {
        return expiracaoSegundos;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(expiracaoSegundos))
                .subject(usuario.getId().toString())
                .claim("role", usuario.getPapel().name())
                .claim("nome", usuario.getNome());

        if (usuario.getOrgao() != null) {
            claims.claim("orgaoId", usuario.getOrgao().getId());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}