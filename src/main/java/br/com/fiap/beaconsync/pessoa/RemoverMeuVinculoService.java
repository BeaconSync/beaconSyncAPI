package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.auditoria.AuditoriaService;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.exception.ConflitoException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RemoverMeuVinculoService {
    private final AuthorizationService authorization;
    private final PessoaVulneravelRepository pessoas;
    private final VinculoResponsavelRepository vinculos;
    private final AuditoriaService auditoria;

    @Transactional
    public void remover(Long pessoaId) {
        Long usuarioId = authorization.usuarioLogadoId();
        if (pessoaId == null || pessoaId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de pessoa inválido.");
        }
        // Serializa saídas da mesma pessoa para duas contas não removerem o último vínculo.
        // A ausência não revela se o cadastro existe e permite repetir um DELETE já confirmado.
        if (pessoas.buscarParaRemoverVinculo(pessoaId).isEmpty()) return;
        var meuVinculo = vinculos.findByPessoaIdAndUsuarioId(pessoaId, usuarioId).orElse(null);
        if (meuVinculo == null) return;
        if (meuVinculo.getStatus() != StatusVinculo.ATIVO) {
            throw new AccessDeniedException("Você não possui vínculo ativo com esta pessoa.");
        }
        if (vinculos.countByPessoaIdAndStatus(pessoaId, StatusVinculo.ATIVO) <= 1) {
            throw new ConflitoException(
                    "Você é a última pessoa com vínculo ativo. Aprove outro responsável antes de parar de acompanhar.",
                    null);
        }
        auditoria.registrar(usuarioId, "REMOVER_PROPRIO_VINCULO", "VINCULO_RESPONSAVEL",
                meuVinculo.getId(), "Pessoa " + pessoaId);
        vinculos.delete(meuVinculo);
    }
}
