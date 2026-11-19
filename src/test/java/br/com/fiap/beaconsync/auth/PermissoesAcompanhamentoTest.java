package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.alerta.*;
import br.com.fiap.beaconsync.pessoa.*;
import br.com.fiap.beaconsync.usuario.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissoesAcompanhamentoTest {
    @Configuration
    @EnableMethodSecurity
    static class Seguranca {}

    private AnnotationConfigApplicationContext contexto;

    @BeforeEach void iniciar() {
        contexto = new AnnotationConfigApplicationContext();
        contexto.register(Seguranca.class);
        contexto.registerBean(PessoaService.class, () -> mock(PessoaService.class));
        contexto.registerBean(EnderecoService.class, () -> mock(EnderecoService.class));
        contexto.registerBean(VinculoService.class, () -> mock(VinculoService.class));
        contexto.registerBean(RemoverMeuVinculoService.class, () -> mock(RemoverMeuVinculoService.class));
        contexto.registerBean(MinhasSolicitacoesService.class, () -> mock(MinhasSolicitacoesService.class));
        contexto.registerBean(AlertaService.class, () -> mock(AlertaService.class));
        contexto.register(PessoaController.class, EnderecoController.class,
                VinculoController.class, RespostaAlertaController.class);
        contexto.refresh();
    }

    @AfterEach void limpar() {
        SecurityContextHolder.clearContext();
        contexto.close();
    }

    @Test void responsavelEVoluntarioPodemAcessarFluxosDeAcompanhamento() {
        for (String papel : List.of("RESPONSAVEL", "VOLUNTARIO")) {
            autenticar(papel);
            assertDoesNotThrow(() -> contexto.getBean(PessoaController.class).buscarPorId(1L));
            assertDoesNotThrow(() -> contexto.getBean(EnderecoController.class).listar(1L));
            assertDoesNotThrow(() -> contexto.getBean(VinculoController.class).pendentes());
            assertEquals(204, contexto.getBean(VinculoController.class).removerMeuVinculo(1L).getStatusCode().value());
            assertDoesNotThrow(() -> contexto.getBean(VinculoController.class).minhasSolicitacoes(0, 50));
            assertDoesNotThrow(() -> contexto.getBean(RespostaAlertaController.class).minhasSituacoes());
        }
    }

    @Test void papelInstitucionalNaoGanhaAcessoAosFluxosPessoais() {
        autenticar("AGENTE_ORGAO");
        assertThrows(AccessDeniedException.class, () -> contexto.getBean(PessoaController.class).buscarPorId(1L));
        assertThrows(AccessDeniedException.class, () -> contexto.getBean(EnderecoController.class).listar(1L));
        assertThrows(AccessDeniedException.class, () -> contexto.getBean(VinculoController.class).pendentes());
        assertThrows(AccessDeniedException.class, () -> contexto.getBean(VinculoController.class).removerMeuVinculo(1L));
        assertThrows(AccessDeniedException.class, () -> contexto.getBean(VinculoController.class).minhasSolicitacoes(0, 50));
        assertThrows(AccessDeniedException.class, () -> contexto.getBean(RespostaAlertaController.class).minhasSituacoes());
    }

    @Test void serVoluntarioNaoSubstituiVinculoAtivoComPessoa() {
        var jwt = Jwt.withTokenValue("teste").header("alg", "HS256").subject("7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_VOLUNTARIO"))));
        var vinculos = mock(VinculoResponsavelRepository.class);
        var auth = new AuthorizationService(mock(UsuarioRepository.class), vinculos);
        assertThrows(AccessDeniedException.class, () -> auth.exigirVinculoAtivo(1L));
        when(vinculos.existsByPessoaIdAndUsuarioIdAndStatus(1L, 7L, StatusVinculo.ATIVO)).thenReturn(true);
        assertDoesNotThrow(() -> auth.exigirVinculoAtivo(1L));
    }

    private void autenticar(String papel) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "7", "", List.of(new SimpleGrantedAuthority("ROLE_" + papel))));
    }
}
