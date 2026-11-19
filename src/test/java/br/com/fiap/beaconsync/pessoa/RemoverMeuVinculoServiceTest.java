package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.auditoria.AuditoriaService;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.exception.ConflitoException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemoverMeuVinculoServiceTest {
    final AuthorizationService auth = mock(AuthorizationService.class);
    final PessoaVulneravelRepository pessoas = mock(PessoaVulneravelRepository.class);
    final VinculoResponsavelRepository vinculos = mock(VinculoResponsavelRepository.class);
    final AuditoriaService audit = mock(AuditoriaService.class);
    final RemoverMeuVinculoService service = new RemoverMeuVinculoService(auth, pessoas, vinculos, audit);

    RemoverMeuVinculoServiceTest() {
        when(auth.usuarioLogadoId()).thenReturn(7L);
        when(pessoas.buscarParaRemoverVinculo(1L)).thenReturn(Optional.of(new PessoaVulneravel()));
    }
    VinculoResponsavel preparar(StatusVinculo status, long ativos) {
        var v = new VinculoResponsavel(); v.setId(10L); v.setStatus(status);
        when(vinculos.findByPessoaIdAndUsuarioId(1L, 7L)).thenReturn(Optional.of(v));
        when(vinculos.countByPessoaIdAndStatus(1L, StatusVinculo.ATIVO)).thenReturn(ativos);
        return v;
    }
    @Test void removeSomenteVinculoDaSessaoEAudita() {
        var meu = preparar(StatusVinculo.ATIVO, 2);
        service.remover(1L);
        verify(vinculos).delete(meu);
        verify(audit).registrar(7L, "REMOVER_PROPRIO_VINCULO", "VINCULO_RESPONSAVEL", 10L, "Pessoa 1");
        verify(pessoas, never()).delete(any());
    }
    @Test void ultimoAtivoBloqueadoMesmoComPedidosPendentes() {
        preparar(StatusVinculo.ATIVO, 1);
        assertThrows(ConflitoException.class, () -> service.remover(1L));
        verify(vinculos, never()).delete(any()); verifyNoInteractions(audit);
    }
    @Test void ausenteOuRepetidoNaoRemoveVinculosAlheios() {
        service.remover(1L);
        verify(vinculos, never()).delete(any()); verifyNoInteractions(audit);
    }
    @Test void pessoaInexistenteTambemRetornaSemAlteracao() {
        service.remover(999L);
        verifyNoInteractions(vinculos, audit);
    }
    @Test void pedidosPendentesERecusadosNaoSaoRemovidosPorEstaOperacao() {
        for (var status : new StatusVinculo[]{StatusVinculo.PENDENTE, StatusVinculo.RECUSADO}) {
            preparar(status, 2);
            assertThrows(AccessDeniedException.class, () -> service.remover(1L));
        }
        verify(vinculos, never()).delete(any()); verifyNoInteractions(audit);
    }
    @Test void codigoInvalidoNaoConsultaBanco() {
        assertThrows(ResponseStatusException.class, () -> service.remover(0L));
        assertThrows(ResponseStatusException.class, () -> service.remover(null));
        verifyNoInteractions(pessoas, vinculos, audit);
    }
}
