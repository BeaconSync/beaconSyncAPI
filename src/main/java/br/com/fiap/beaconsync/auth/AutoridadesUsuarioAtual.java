package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AutoridadesUsuarioAtual implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final UsuarioRepository usuarios;

    @Override
    @Transactional(readOnly = true)
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        final long id;
        try {
            id = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw tokenInvalido();
        }
        var usuario = usuarios.findById(id)
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()) && u.getPapel() != null)
                .orElseThrow(this::tokenInvalido);
        // A assinatura/validade continua validada pelo JwtDecoder. O papel é o atual,
        // evitando que um JWT emitido antes da troca mantenha permissões antigas.
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().name()));
    }

    private OAuth2AuthenticationException tokenInvalido() {
        return new OAuth2AuthenticationException(new OAuth2Error(
                "invalid_token", "Usuário não encontrado ou inativo.", null));
    }
}
