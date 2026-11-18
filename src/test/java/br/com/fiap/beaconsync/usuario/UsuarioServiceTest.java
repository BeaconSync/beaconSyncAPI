package br.com.fiap.beaconsync.usuario;

import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.exception.RegraNegocioException;
import br.com.fiap.beaconsync.resgate.*;
import br.com.fiap.beaconsync.voluntariado.NetIndisponivelException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {
    private final AuthorizationService auth = mock(AuthorizationService.class);
    private final ResgatistaRepository resgatistas = mock(ResgatistaRepository.class);
    private final ParticipacaoResgateRepository participacoes = mock(ParticipacaoResgateRepository.class);
    private final DisponibilidadeService disponibilidade = mock(DisponibilidadeService.class);
    private final EntityManager em = mock(EntityManager.class);
    private final UsuarioService service = new UsuarioService(auth, resgatistas, participacoes, disponibilidade, em);
    private Usuario usuario;

    @BeforeEach void preparar() {
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNome("Nome original");
        usuario.setPapel(Papel.RESPONSAVEL);
        when(auth.usuarioLogado()).thenReturn(usuario);
        when(resgatistas.findByUsuarioId(7L)).thenReturn(Optional.empty());
    }

    @Test void clienteAntigoPodeEditarSemEnviarPapel() {
        var resposta = service.atualizarMeuPerfil(new AtualizarPerfilDTO(" Ana ", "11999999999"));
        assertEquals("Ana", resposta.nome());
        assertEquals(Papel.RESPONSAVEL, resposta.papel());
        verifyNoInteractions(resgatistas, participacoes, disponibilidade);
    }

    @Test void criaVoluntarioIndisponivelAoMudarDeResponsavel() {
        var resposta = service.atualizarMeuPerfil(dto(Papel.VOLUNTARIO));
        var captor = ArgumentCaptor.forClass(Resgatista.class);
        verify(resgatistas).save(captor.capture());
        assertSame(usuario, captor.getValue().getUsuario());
        assertEquals(TipoResgatista.VOLUNTARIO, captor.getValue().getTipo());
        assertFalse(captor.getValue().getDisponivel());
        assertEquals(Papel.VOLUNTARIO, resposta.papel());
    }

    @Test void preservaCadastroExistenteAoVoltarASerVoluntario() {
        Resgatista existente = resgatista();
        service.atualizarMeuPerfil(dto(Papel.VOLUNTARIO));
        verify(disponibilidade).pausarParaTrocaDePerfil(existente);
        verify(resgatistas, never()).save(any());
        verify(resgatistas, never()).delete(any());
    }

    @Test void pausaVoluntariadoSemApagarHistoricoAoVirarResponsavel() {
        usuario.setPapel(Papel.VOLUNTARIO);
        Resgatista existente = resgatista();
        var resposta = service.atualizarMeuPerfil(dto(Papel.RESPONSAVEL));
        assertEquals(Papel.RESPONSAVEL, resposta.papel());
        verify(disponibilidade).pausarParaTrocaDePerfil(existente);
        verify(resgatistas, never()).delete(any());
    }

    @Test void resgateAbertoImpedeTroca() {
        usuario.setPapel(Papel.VOLUNTARIO);
        when(participacoes.existsByResgatistaUsuarioIdAndOcorrenciaStatusIn(eq(7L), anyCollection()))
                .thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> service.atualizarMeuPerfil(dto(Papel.RESPONSAVEL)));
        assertEquals(Papel.VOLUNTARIO, usuario.getPapel());
        assertEquals("Nome original", usuario.getNome());
        verifyNoInteractions(disponibilidade);
    }

    @Test void falhaDoNetNaoConfirmaTroca() {
        usuario.setPapel(Papel.VOLUNTARIO);
        Resgatista existente = resgatista();
        doThrow(new NetIndisponivelException()).when(disponibilidade).pausarParaTrocaDePerfil(existente);
        assertThrows(NetIndisponivelException.class, () -> service.atualizarMeuPerfil(dto(Papel.RESPONSAVEL)));
        assertEquals(Papel.VOLUNTARIO, usuario.getPapel());
        assertEquals("Nome original", usuario.getNome());
    }

    @Test void naoPermiteAutopromocaoParaPapelInstitucional() {
        for (Papel destino : new Papel[]{Papel.PROFISSIONAL, Papel.AGENTE_ORGAO}) {
            assertThrows(RegraNegocioException.class, () -> service.atualizarMeuPerfil(dto(destino)));
            assertEquals(Papel.RESPONSAVEL, usuario.getPapel());
        }
        verifyNoInteractions(disponibilidade);
    }

    @Test void contaInstitucionalNaoPodeTrocarPapelNestaOpcao() {
        usuario.setPapel(Papel.PROFISSIONAL);
        assertThrows(RegraNegocioException.class, () -> service.atualizarMeuPerfil(dto(Papel.VOLUNTARIO)));
    }

    private Resgatista resgatista() {
        var r = new Resgatista();
        r.setUsuario(usuario);
        r.setTipo(TipoResgatista.VOLUNTARIO);
        when(resgatistas.findByUsuarioId(7L)).thenReturn(Optional.of(r));
        return r;
    }

    private AtualizarPerfilDTO dto(Papel papel) {
        return new AtualizarPerfilDTO("Ana", "11999999999", papel);
    }
}
