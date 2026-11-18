package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.usuario.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutoridadesUsuarioAtualTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AutoridadesUsuarioAtual converter = new AutoridadesUsuarioAtual(usuarios);

    @Test void tokenAntigoUsaPapelAtualDoBanco() {
        var usuario = new Usuario();
        usuario.setPapel(Papel.RESPONSAVEL);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        assertEquals("ROLE_RESPONSAVEL", converter.convert(jwt()).iterator().next().getAuthority());
        usuario.setPapel(Papel.VOLUNTARIO);
        assertEquals("ROLE_VOLUNTARIO", converter.convert(jwt()).iterator().next().getAuthority());
    }

    @Test void usuarioInativoOuRemovidoNaoPodeAutenticar() {
        when(usuarios.findById(7L)).thenReturn(Optional.empty());
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(jwt()));
        var usuario = new Usuario();
        usuario.setPapel(Papel.VOLUNTARIO);
        usuario.setAtivo(false);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(jwt()));
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("teste").header("alg", "HS256")
                .subject("7").claim("role", "VOLUNTARIO").build();
    }
}
