package br.com.fiap.beaconsync.voluntariado;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NetTokenService {
    private final JwtEncoder encoder;
    private final String issuer;

    public NetTokenService(JwtEncoder encoder, @Value("${beaconsync.jwt.issuer}") String issuer) {
        this.encoder = encoder;
        this.issuer = issuer;
    }

    public String emitir(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0)
            throw new IllegalArgumentException("Identidade inválida.");
        var agora = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(usuarioId.toString())
                        .issuedAt(agora)
                        .expiresAt(agora.plusSeconds(60))
                        .id(UUID.randomUUID().toString())
                        .audience(List.of("beaconsync-net"))
                        .claim("purpose", "voluntariado-interno")
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}