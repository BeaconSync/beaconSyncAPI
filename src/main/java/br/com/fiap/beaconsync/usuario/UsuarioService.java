package br.com.fiap.beaconsync.usuario;

import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.exception.RegraNegocioException;
import br.com.fiap.beaconsync.resgate.DisponibilidadeService;
import br.com.fiap.beaconsync.resgate.ParticipacaoResgateRepository;
import br.com.fiap.beaconsync.resgate.Resgatista;
import br.com.fiap.beaconsync.resgate.ResgatistaRepository;
import br.com.fiap.beaconsync.resgate.StatusOcorrencia;
import br.com.fiap.beaconsync.resgate.TipoResgatista;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final AuthorizationService authorizationService;
    private final ResgatistaRepository resgatistas;
    private final ParticipacaoResgateRepository participacoes;
    private final DisponibilidadeService disponibilidade;
    private final EntityManager entityManager;
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public UsuarioResponseDTO dadosDoUsuarioLogado() {
        return new UsuarioResponseDTO(authorizationService.usuarioLogado());
    }

    @Transactional
    public UsuarioResponseDTO atualizarMeuPerfil(AtualizarPerfilDTO dto) {
        Usuario usuario = authorizationService.usuarioLogado();
        // Serializa a troca com outras edições e com a escala em um resgate.
        entityManager.refresh(usuario, LockModeType.PESSIMISTIC_WRITE);
        if (dto.email() != null) {
            usuarios.findByEmailIgnoreCase(dto.email())
                    .filter(outro -> !outro.getId().equals(usuario.getId()))
                    .ifPresent(outro -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Este e-mail já está em uso. Informe outro e-mail.");
                    });
        }
        if (dto.papel() != null && dto.papel() != usuario.getPapel()) {
            alterarPapel(usuario, dto.papel());
        }
        usuario.setNome(dto.nome().trim());
        usuario.setTelefone(dto.telefone());
        if (dto.email() != null) usuario.setEmail(dto.email());
        return new UsuarioResponseDTO(usuario);
    }

    private void alterarPapel(Usuario usuario, Papel destino) {
        if (!papelPublico(usuario.getPapel()) || !papelPublico(destino)) {
            throw new RegraNegocioException("Só é possível alternar entre RESPONSAVEL e VOLUNTARIO.");
        }
        if (destino == Papel.RESPONSAVEL
                && participacoes.existsByResgatistaUsuarioIdAndOcorrenciaStatusIn(usuario.getId(),
                List.of(StatusOcorrencia.SOLICITADA, StatusOcorrencia.EM_ANDAMENTO))) {
            throw new RegraNegocioException(
                    "Finalize sua participação nos resgates em aberto antes de mudar para responsável.");
        }
        Resgatista resgatista = resgatistas.findByUsuarioId(usuario.getId()).orElse(null);
        if (resgatista != null) {
            if (resgatista.getTipo() != TipoResgatista.VOLUNTARIO) {
                throw new RegraNegocioException("Um perfil profissional não pode ser alterado por esta opção.");
            }
            disponibilidade.pausarParaTrocaDePerfil(resgatista);
        } else if (destino == Papel.VOLUNTARIO) {
            resgatista = new Resgatista();
            resgatista.setUsuario(usuario);
            resgatista.setTipo(TipoResgatista.VOLUNTARIO);
            resgatista.setDisponivel(false);
            resgatistas.save(resgatista);
        }
        // Não remove pessoas, vínculos, cadastro de voluntário nem histórico de resgates.
        usuario.setPapel(destino);
    }

    private boolean papelPublico(Papel papel) {
        return papel == Papel.RESPONSAVEL || papel == Papel.VOLUNTARIO;
    }
}
