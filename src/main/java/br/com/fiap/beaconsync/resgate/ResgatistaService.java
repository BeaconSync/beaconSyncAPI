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

    @Transactional(readOnly = true)
    public ResgatistaResponseDTO meuPerfil() {
        return new ResgatistaResponseDTO(buscarDoUsuarioLogado());
    }

    /** O voluntário avisa que pode ajudar (e em qual região). */
    @Transactional
    public ResgatistaResponseDTO atualizarDisponibilidade(DisponibilidadeRequestDTO dto) {
        Resgatista resgatista = buscarDoUsuarioLogado();
        resgatista.setDisponivel(dto.disponivel());
        if (dto.distritoBaseId() != null) {
            resgatista.setDistritoBase(distritoRepository.findById(dto.distritoBaseId())
                    .orElseThrow(() -> new RegraNegocioException("Distrito " + dto.distritoBaseId() + " não existe.")));
        }
        if (dto.habilidades() != null) {
            resgatista.setHabilidades(dto.habilidades().trim());
        }
        return new ResgatistaResponseDTO(resgatista);
    }

    /**
     * Quem o agente pode escalar: voluntários disponíveis e profissionais do
     * próprio órgão. Filtro opcional pela região do voluntário.
     */
    @Transactional(readOnly = true)
    public List<ResgatistaResponseDTO> disponiveis(Long distritoId) {
        Usuario agente = authorizationService.usuarioLogado();
        Long orgaoId = agente.getOrgao() != null ? agente.getOrgao().getId() : null;

        return resgatistaRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getDisponivel())
                        || (r.getTipo() == TipoResgatista.PROFISSIONAL && r.getOrgao() != null
                        && r.getOrgao().getId().equals(orgaoId)))
                .filter(r -> distritoId == null || r.getTipo() == TipoResgatista.PROFISSIONAL
                        || (r.getDistritoBase() != null && r.getDistritoBase().getId().equals(distritoId)))
                .map(ResgatistaResponseDTO::new)
                .sorted(Comparator.comparing(ResgatistaResponseDTO::tipo).thenComparing(ResgatistaResponseDTO::nome))
                .toList();
    }

    private Resgatista buscarDoUsuarioLogado() {
        return resgatistaRepository.findByUsuarioId(authorizationService.usuarioLogadoId())
                .orElseThrow(() -> new EntityNotFoundException("Seu usuário não tem perfil de resgatista."));
    }
}