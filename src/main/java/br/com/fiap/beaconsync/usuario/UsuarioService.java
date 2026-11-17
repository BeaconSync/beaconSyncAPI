package br.com.fiap.beaconsync.usuario;

import br.com.fiap.beaconsync.auth.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public UsuarioResponseDTO dadosDoUsuarioLogado() {
        return new UsuarioResponseDTO(authorizationService.usuarioLogado());
    }

    @Transactional
    public UsuarioResponseDTO atualizarMeuPerfil(AtualizarPerfilDTO dto) {
        Usuario usuario = authorizationService.usuarioLogado();
        usuario.setNome(dto.nome().trim());
        usuario.setTelefone(dto.telefone());
        return new UsuarioResponseDTO(usuario);
    }
}