package br.com.fiap.beaconsync.usuario;

import br.com.fiap.beaconsync.auth.AuthorizationService;

import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FotoPerfilService {
    private final FotoPerfilRepository fotos;
    private final AuthorizationService authorization;

    @Transactional
    public UsuarioResponseDTO salvar(byte[] imagem) {
        Usuario usuario = authorization.usuarioLogado();
        var foto = new FotoPerfil();
        foto.setUsuarioId(usuario.getId());
        foto.setConteudo(imagem);
        fotos.save(foto);
        usuario.setFotoVersao(UUID.randomUUID().toString());
        return new UsuarioResponseDTO(usuario);
    }

    @Transactional(readOnly = true)
    public byte[] consultar() {
        Long id = authorization.usuarioLogado().getId();
        return fotos.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Foto não cadastrada."))
                .getConteudo();
    }

    @Transactional
    public void excluir() {
        Usuario usuario = authorization.usuarioLogado();
        fotos.findById(usuario.getId()).ifPresent(fotos::delete);
        usuario.setFotoVersao(null);
    }
}