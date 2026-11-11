package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.auditoria.AuditoriaService;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.exception.ConflitoException;
import br.com.fiap.beaconsync.exception.RegraNegocioException;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VinculoService {

    private final VinculoResponsavelRepository vinculoRepository;
    private final PessoaVulneravelRepository pessoaRepository;
    private final AuthorizationService authorizationService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public VinculoResponseDTO solicitar(Long pessoaId, VinculoRequestDTO dto) {
        Usuario usuario = authorizationService.usuarioLogado();
        PessoaVulneravel pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));

        VinculoResponsavel vinculo = vinculoRepository.findByPessoaIdAndUsuarioId(pessoaId, usuario.getId())
                .orElse(null);

        if (vinculo != null && vinculo.getStatus() == StatusVinculo.ATIVO) {
            throw new ConflitoException("Você já é responsável por esta pessoa.", vinculo.getId());
        }
        if (vinculo != null && vinculo.getStatus() == StatusVinculo.PENDENTE) {
            throw new ConflitoException("Seu pedido de vínculo já foi enviado e aguarda aprovação.", vinculo.getId());
        }
        if (vinculo == null) {
            vinculo = new VinculoResponsavel();
            vinculo.setPessoa(pessoa);
            vinculo.setUsuario(usuario);
        }
        // Pedido novo, ou novo pedido depois de uma recusa
        vinculo.setParentesco(dto.parentesco().trim());
        vinculo.setStatus(StatusVinculo.PENDENTE);
        vinculoRepository.save(vinculo);

        auditoriaService.registrar(usuario.getId(), "SOLICITAR_VINCULO", "VINCULO_RESPONSAVEL", vinculo.getId(),
                "Pedido de vínculo com a pessoa " + pessoaId);
        return new VinculoResponseDTO(vinculo);
    }

    @Transactional(readOnly = true)
    public List<VinculoResponseDTO> listarDaPessoa(Long pessoaId) {
        authorizationService.exigirVinculoAtivo(pessoaId);
        return vinculoRepository.findByPessoaIdOrderByCriadoEmAsc(pessoaId).stream()
                .map(VinculoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VinculoResponseDTO> pendentesParaAprovar() {
        return vinculoRepository
                .findPendentesParaAprovar(authorizationService.usuarioLogadoId(),
                        StatusVinculo.PENDENTE, StatusVinculo.ATIVO)
                .stream()
                .map(VinculoResponseDTO::new)
                .toList();
    }

    @Transactional
    public VinculoResponseDTO decidir(Long vinculoId, VinculoDecisaoDTO dto) {
        VinculoResponsavel vinculo = vinculoRepository.findById(vinculoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido de vínculo não encontrado."));

        authorizationService.exigirVinculoAtivo(vinculo.getPessoa().getId());
        if (vinculo.getStatus() != StatusVinculo.PENDENTE) {
            throw new RegraNegocioException("Este pedido já foi respondido.");
        }

        vinculo.setStatus(Boolean.TRUE.equals(dto.aprovar()) ? StatusVinculo.ATIVO : StatusVinculo.RECUSADO);

        auditoriaService.registrar(authorizationService.usuarioLogadoId(),
                Boolean.TRUE.equals(dto.aprovar()) ? "APROVAR_VINCULO" : "RECUSAR_VINCULO",
                "VINCULO_RESPONSAVEL", vinculoId, "Pessoa " + vinculo.getPessoa().getId());
        return new VinculoResponseDTO(vinculo);
    }
}