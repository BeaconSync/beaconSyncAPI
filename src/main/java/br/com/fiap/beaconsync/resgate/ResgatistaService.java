package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.distrito.DistritoRepository;
import br.com.fiap.beaconsync.exception.RegraNegocioException;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResgatistaService {

    private final ResgatistaRepository resgatistaRepository;
    private final DistritoRepository distritoRepository;
    private final AuthorizationService authorizationService;
    private final DisponibilidadeService disponibilidadeService;

    @Transactional(readOnly = true)
    public ResgatistaResponseDTO meuPerfil() {
        Resgatista resgatista = buscarDoUsuarioLogado();
        return new ResgatistaResponseDTO(resgatista, disponibilidadeService.disponivel(resgatista));
    }

    @Transactional
    public ResgatistaResponseDTO atualizarDisponibilidade(DisponibilidadeRequestDTO dto) {
        Resgatista resgatista = buscarDoUsuarioLogado();
        if (dto.distritoBaseId() != null) {
            resgatista.setDistritoBase(distritoRepository.findById(dto.distritoBaseId())
                    .orElseThrow(() -> new RegraNegocioException("Distrito " + dto.distritoBaseId() + " não existe.")));
        }
        if (dto.habilidades() != null) {
            resgatista.setHabilidades(dto.habilidades().trim());
        }
        boolean disponivel = disponibilidadeService.alterar(resgatista, dto.disponivel());
        return new ResgatistaResponseDTO(resgatista, disponivel);
    }

    @Transactional(readOnly = true)
    public List<ResgatistaResponseDTO> disponiveis(Long distritoId) {
        Usuario agente = authorizationService.usuarioLogado();
        Long orgaoId = agente.getOrgao() != null ? agente.getOrgao().getId() : null;

        return resgatistaRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getUsuario().getAtivo()))
                .filter(r -> r.getTipo() == TipoResgatista.VOLUNTARIO
                        || Boolean.TRUE.equals(r.getDisponivel())
                        || (r.getOrgao() != null && r.getOrgao().getId().equals(orgaoId)))
                .filter(r -> distritoId == null || r.getTipo() == TipoResgatista.PROFISSIONAL
                        || (r.getDistritoBase() != null && r.getDistritoBase().getId().equals(distritoId)))
                .map(r -> new ResgatistaResponseDTO(r, r.getTipo() == TipoResgatista.PROFISSIONAL
                        ? Boolean.TRUE.equals(r.getDisponivel())
                        : disponibilidadeService.disponivelParaListagem(r)))
                .filter(r -> r.tipo() == TipoResgatista.PROFISSIONAL || r.disponivel())
                .sorted(Comparator.comparing(ResgatistaResponseDTO::tipo).thenComparing(ResgatistaResponseDTO::nome))
                .toList();
    }

    private Resgatista buscarDoUsuarioLogado() {
        return resgatistaRepository.findByUsuarioId(authorizationService.usuarioLogadoId())
                .orElseThrow(() -> new EntityNotFoundException("Seu usuário não tem perfil de resgatista."));
    }
}