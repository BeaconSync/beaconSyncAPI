package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.auditoria.AuditoriaService;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.exception.ConflitoException;
import br.com.fiap.beaconsync.usuario.Papel;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "beaconsync.seed.enabled=false", "beaconsync.seed.demo=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RemoverMeuVinculoIntegracaoTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired PessoaVulneravelRepository pessoas;
    @Autowired VinculoResponsavelRepository vinculos;

    @Test void duasSaidasConcorrentesMantemUmAtivo() throws Exception {
        var tx = new TransactionTemplate(manager);
        Long[] ids = tx.execute(s -> {
            var pessoa = new PessoaVulneravel(); pessoa.setNome("Pessoa teste");
            pessoa.setCpf(String.format("%011d", System.nanoTime() % 100000000000L));
            pessoa.setDataNascimento(LocalDate.of(1960, 1, 1));
            pessoa.setTipoVulnerabilidade(TipoVulnerabilidade.OUTRO);
            pessoa.setConsentimentoEm(LocalDateTime.now()); em.persist(pessoa);
            Long[] resultado = new Long[3]; resultado[0] = pessoa.getId();
            for (int i = 1; i <= 2; i++) {
                var u = new Usuario(); u.setNome("Teste"); u.setEmail(UUID.randomUUID()+"@example.com");
                u.setSenhaHash("teste"); u.setPapel(Papel.RESPONSAVEL); em.persist(u);
                var v = new VinculoResponsavel(); v.setPessoa(pessoa); v.setUsuario(u);
                v.setParentesco("Familiar"); v.setStatus(StatusVinculo.ATIVO); em.persist(v);
                resultado[i] = u.getId();
            }
            return resultado;
        });
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> sair(ids[0], ids[1], inicio));
            var b = pool.submit(() -> sair(ids[0], ids[2], inicio));
            inicio.countDown();
            assertEquals(1, a.get(15, TimeUnit.SECONDS) + b.get(15, TimeUnit.SECONDS));
        }
        tx.executeWithoutResult(s -> {
            assertTrue(pessoas.existsById(ids[0]));
            assertEquals(1, vinculos.countByPessoaIdAndStatus(ids[0], StatusVinculo.ATIVO));
            var restante = vinculos.findByPessoaIdAndStatus(ids[0], StatusVinculo.ATIVO).getFirst();
            Long removido = restante.getUsuario().getId().equals(ids[1]) ? ids[2] : ids[1];
            service(removido).remover(ids[0]); // repetição não exclui o responsável restante
            assertEquals(1, vinculos.countByPessoaIdAndStatus(ids[0], StatusVinculo.ATIVO));
        });
    }
    int sair(Long pessoaId, Long usuarioId, CountDownLatch inicio) throws Exception {
        if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout teste");
        try {
            new TransactionTemplate(manager).executeWithoutResult(s -> service(usuarioId).remover(pessoaId));
            return 1;
        } catch (ConflitoException esperado) { return 0; }
    }
    RemoverMeuVinculoService service(Long usuarioId) {
        var auth = mock(AuthorizationService.class); when(auth.usuarioLogadoId()).thenReturn(usuarioId);
        return new RemoverMeuVinculoService(auth, pessoas, vinculos, mock(AuditoriaService.class));
    }
}
