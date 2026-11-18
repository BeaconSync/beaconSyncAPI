package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.usuario.*;
import br.com.fiap.beaconsync.voluntariado.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisponibilidadeServiceTest {
    private final VoluntariadoClient net = mock(VoluntariadoClient.class);

    @Test void responsavelNaoFicaDisponivelMesmoComCadastroAntigo() {
        var r = resgatista();
        r.getUsuario().setPapel(Papel.RESPONSAVEL);
        r.setDisponivel(true);
        assertFalse(service(true).disponivel(r));
        verifyNoInteractions(net);
    }

    @Test void pausaPerfilExistenteNoNet() {
        var r = resgatista();
        when(net.consultar(7L)).thenReturn(Optional.of(new VoluntariadoClient.Perfil("7", "teste", true)));
        when(net.alterarDisponibilidade(7L, false)).thenReturn(new VoluntariadoClient.Perfil("7", "teste", false));
        service(true).pausarParaTrocaDePerfil(r);
        assertFalse(r.getDisponivel());
        verify(net).alterarDisponibilidade(7L, false);
    }

    @Test void cadastroAusenteNoNetNaoImpedePausa() {
        when(net.consultar(7L)).thenReturn(Optional.empty());
        service(true).pausarParaTrocaDePerfil(resgatista());
        verify(net, never()).alterarDisponibilidade(7L, false);
    }

    @Test void integracaoDesligadaPausaSomenteNoJava() {
        var r = resgatista();
        service(false).pausarParaTrocaDePerfil(r);
        assertFalse(r.getDisponivel());
        verifyNoInteractions(net);
    }

    @Test void respostaDoNetAindaDisponivelNaoConfirmaPausa() {
        when(net.consultar(7L)).thenReturn(Optional.of(new VoluntariadoClient.Perfil("7", "teste", true)));
        when(net.alterarDisponibilidade(7L, false)).thenReturn(new VoluntariadoClient.Perfil("7", "teste", true));
        assertThrows(NetIndisponivelException.class, () -> service(true).pausarParaTrocaDePerfil(resgatista()));
    }

    private DisponibilidadeService service(boolean integracao) {
        var p = new NetProperties(); p.setIntegracao(integracao);
        return new DisponibilidadeService(net, p);
    }
    private Resgatista resgatista() {
        var u = new Usuario(); u.setId(7L); u.setPapel(Papel.VOLUNTARIO);
        var r = new Resgatista(); r.setUsuario(u); r.setTipo(TipoResgatista.VOLUNTARIO); r.setDisponivel(true);
        return r;
    }
}
