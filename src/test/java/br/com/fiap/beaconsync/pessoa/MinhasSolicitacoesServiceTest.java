package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.auth.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MinhasSolicitacoesServiceTest {
    @Test void usaSomenteOUsuarioAutenticadoEConverteResultado() {
        var auth = mock(AuthorizationService.class);
        var repo = mock(MinhasSolicitacoesRepository.class);
        var item = mock(MinhasSolicitacoesRepository.Pedido.class);
        var data = LocalDateTime.of(2026, 11, 18, 10, 0);
        when(auth.usuarioLogadoId()).thenReturn(7L);
        when(item.getId()).thenReturn(10L);
        when(item.getParentesco()).thenReturn("Familiar");
        when(item.getStatus()).thenReturn("ATIVO");
        when(item.getCriadaEm()).thenReturn(data);
        when(item.getDecididaEm()).thenReturn(data.plusHours(1));
        when(repo.consultar(7L, PageRequest.of(0, 50))).thenReturn(new PageImpl<>(List.of(item)));
        var resultado = new MinhasSolicitacoesService(auth, repo).consultar(0, 50).getContent().getFirst();
        assertEquals(new MinhaSolicitacaoVinculoDTO(10L, "Familiar", StatusVinculo.ATIVO,
                data, data.plusHours(1)), resultado);
        verify(repo).consultar(7L, PageRequest.of(0, 50));
    }

    @Test void rejeitaPaginacaoInvalidaSemConsultarBanco() {
        var auth = mock(AuthorizationService.class);
        var repo = mock(MinhasSolicitacoesRepository.class);
        var service = new MinhasSolicitacoesService(auth, repo);
        for (int[] par : new int[][]{{-1,50}, {0,0}, {0,101}}) {
            assertEquals(400, assertThrows(ResponseStatusException.class,
                    () -> service.consultar(par[0], par[1])).getStatusCode().value());
        }
        verifyNoInteractions(repo);
    }
}
