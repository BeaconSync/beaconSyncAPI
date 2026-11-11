package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.pessoa.StatusVinculo;
import br.com.fiap.beaconsync.pessoa.VinculoResponsavelRepository;
import br.com.fiap.beaconsync.usuario.Papel;
import br.com.fiap.beaconsync.usuario.Usuario;
import br.com.fiap.beaconsync.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationService {

    private final UsuarioRepository usuarioRepository;
    private final VinculoResponsavelRepository vinculoRepository;

    public Long usuarioLogadoId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        return Long.valueOf(jwt.getSubject());
    }

    public Usuario usuarioLogado() {
        return usuarioRepository.findById(usuarioLogadoId())
                .filter(usuario -> Boolean.TRUE.equals(usuario.getAtivo()))
                .orElseThrow(() -> new AccessDeniedException("Usuário do token não existe ou está inativo."));
    }

    public boolean temPapel(Papel papel) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + papel.name()));
    }

    /** Lança 403 se o usuário logado não for responsável ATIVO pela pessoa. */
    public void exigirVinculoAtivo(Long pessoaId) {
        boolean vinculado = vinculoRepository.existsByPessoaIdAndUsuarioIdAndStatus(
                pessoaId, usuarioLogadoId(), StatusVinculo.ATIVO);
        if (!vinculado) {
            throw new AccessDeniedException("Você não é responsável por esta pessoa.");
        }
    }
}