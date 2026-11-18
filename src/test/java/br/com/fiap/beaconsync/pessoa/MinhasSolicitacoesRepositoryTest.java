package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.auditoria.AuditLog;
import br.com.fiap.beaconsync.usuario.Papel;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "beaconsync.seed.enabled=false", "beaconsync.seed.demo=false"
})
class MinhasSolicitacoesRepositoryTest {
    @Autowired EntityManager em;
    @Autowired MinhasSolicitacoesRepository repo;
    private int numero;
    private final LocalDateTime data = LocalDateTime.of(2026, 11, 18, 10, 0);

    @Test void naoListaVinculoAutomaticoNemPedidoDeOutraConta() {
        var dono = usuario(); var outro = usuario();
        vinculo(dono, StatusVinculo.ATIVO);
        var alheio = vinculo(outro, StatusVinculo.PENDENTE);
        evento(alheio, outro, "SOLICITAR_VINCULO", data);
        var meu = vinculo(dono, StatusVinculo.PENDENTE);
        evento(meu, dono, "SOLICITAR_VINCULO", data);
        em.flush();
        var pagina = repo.consultar(dono.getId(), PageRequest.of(0,50));
        assertEquals(1, pagina.getTotalElements());
        assertEquals(meu.getId(), pagina.getContent().getFirst().getId());
        assertNull(pagina.getContent().getFirst().getDecididaEm());
    }

    @Test void listaDecisoesSemExporDadosDaPessoa() {
        var dono = usuario(); var decisor = usuario();
        var aprovado = vinculo(dono, StatusVinculo.ATIVO);
        evento(aprovado, dono, "SOLICITAR_VINCULO", data);
        evento(aprovado, decisor, "APROVAR_VINCULO", data.plusHours(1));
        var recusado = vinculo(dono, StatusVinculo.RECUSADO);
        evento(recusado, dono, "SOLICITAR_VINCULO", data.plusHours(2));
        evento(recusado, decisor, "RECUSAR_VINCULO", data.plusHours(3));
        em.flush();
        var itens = repo.consultar(dono.getId(), PageRequest.of(0,50)).getContent();
        assertEquals(2, itens.size());
        assertEquals("RECUSADO", itens.get(0).getStatus());
        assertEquals(data.plusHours(3), itens.get(0).getDecididaEm());
        assertEquals("ATIVO", itens.get(1).getStatus());
        assertEquals(data.plusHours(1), itens.get(1).getDecididaEm());
        var campos = java.util.Arrays.stream(MinhaSolicitacaoVinculoDTO.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList();
        assertEquals(java.util.List.of("id", "parentesco", "status", "criadaEm", "decididaEm"), campos);
    }

    @Test void reenvioUsaDataNovaSemReaproveitarDecisaoAntiga() {
        var dono = usuario(); var decisor = usuario();
        var pedido = vinculo(dono, StatusVinculo.PENDENTE);
        evento(pedido, dono, "SOLICITAR_VINCULO", data);
        evento(pedido, decisor, "RECUSAR_VINCULO", data.plusHours(1));
        evento(pedido, dono, "SOLICITAR_VINCULO", data.plusHours(2));
        em.flush();
        var pagina = repo.consultar(dono.getId(), PageRequest.of(0,50));
        assertEquals(1, pagina.getTotalElements());
        assertEquals(data.plusHours(2), pagina.getContent().getFirst().getCriadaEm());
        assertNull(pagina.getContent().getFirst().getDecididaEm());
    }

    @Test void auditoriaDeOutroUsuarioNaoTransformaVinculoAutomaticoEmPedido() {
        var dono = usuario(); var outro = usuario();
        var vinculo = vinculo(dono, StatusVinculo.ATIVO);
        evento(vinculo, outro, "SOLICITAR_VINCULO", data);
        em.flush();
        assertEquals(0, repo.consultar(dono.getId(), PageRequest.of(0,50)).getTotalElements());
    }

    @Test void paginacaoContaPedidosSemDuplicarTentativasOuEmpates() {
        var dono = usuario();
        for (int i = 0; i < 3; i++) {
            var pedido = vinculo(dono, StatusVinculo.PENDENTE);
            evento(pedido, dono, "SOLICITAR_VINCULO", data);
            evento(pedido, dono, "SOLICITAR_VINCULO", data);
        }
        em.flush();
        var primeira = repo.consultar(dono.getId(), PageRequest.of(0,2));
        var ultima = repo.consultar(dono.getId(), PageRequest.of(1,2));
        assertEquals(3, primeira.getTotalElements());
        assertEquals(2, primeira.getTotalPages());
        assertEquals(2, primeira.getContent().size());
        assertEquals(1, ultima.getContent().size());
        assertTrue(primeira.getContent().get(1).getId() > ultima.getContent().getFirst().getId());
    }

    @Test void aprovacaoSemEventoDeDecisaoNaoInventaData() {
        var dono = usuario(); var pedido = vinculo(dono, StatusVinculo.ATIVO);
        evento(pedido, dono, "SOLICITAR_VINCULO", data);
        em.flush();
        assertNull(repo.consultar(dono.getId(), PageRequest.of(0,50)).getContent().getFirst().getDecididaEm());
    }

    private Usuario usuario() {
        var usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail("teste" + (++numero) + "@example.com");
        usuario.setSenhaHash("hash-apenas-teste");
        usuario.setPapel(Papel.RESPONSAVEL);
        em.persist(usuario);
        return usuario;
    }

    private VinculoResponsavel vinculo(Usuario usuario, StatusVinculo status) {
        var pessoa = new PessoaVulneravel();
        pessoa.setNome("Nome protegido");
        pessoa.setCpf(String.format("%011d", ++numero));
        pessoa.setDataNascimento(LocalDate.of(1960,1,1));
        pessoa.setTipoVulnerabilidade(TipoVulnerabilidade.OUTRO);
        pessoa.setConsentimentoEm(data);
        em.persist(pessoa);
        var vinculo = new VinculoResponsavel();
        vinculo.setUsuario(usuario); vinculo.setPessoa(pessoa);
        vinculo.setParentesco("Familiar"); vinculo.setStatus(status);
        em.persist(vinculo);
        return vinculo;
    }

    private void evento(VinculoResponsavel vinculo, Usuario usuario, String acao, LocalDateTime momento) {
        var log = new AuditLog();
        log.setEntidade("VINCULO_RESPONSAVEL"); log.setEntidadeId(vinculo.getId());
        log.setUsuario(usuario); log.setAcao(acao); log.setCriadoEm(momento);
        em.persist(log);
    }
}
