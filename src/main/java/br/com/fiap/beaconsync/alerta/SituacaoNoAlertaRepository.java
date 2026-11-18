package br.com.fiap.beaconsync.alerta;

import br.com.fiap.beaconsync.pessoa.StatusVinculo;
import br.com.fiap.beaconsync.pessoa.TipoVulnerabilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SituacaoNoAlertaRepository extends JpaRepository<SituacaoNoAlerta, Long> {

    Optional<SituacaoNoAlerta> findByAlertaIdAndPessoaId(Long alertaId, Long pessoaId);

    List<SituacaoNoAlerta> findByAlertaId(Long alertaId);

    long countByAlertaId(Long alertaId);

    List<SituacaoNoAlerta> findByAlertaIdAndEnderecoDistritoId(Long alertaId, Long distritoId);

    long countByAlertaIdAndEnderecoDistritoIdAndSituacaoIn(Long alertaId, Long distritoId,
                                                           Collection<Situacao> situacoes);

    @Query("""
            select s.endereco.distrito.id as distritoId, s.situacao as situacao, count(s) as total
            from SituacaoNoAlerta s
            where s.alerta.id = :alertaId and s.situacao in :situacoes
            group by s.endereco.distrito.id, s.situacao""")
    List<ContagemDistrito> contarPorDistrito(@Param("alertaId") Long alertaId,
                                             @Param("situacoes") Collection<Situacao> situacoes);

    @Query("""
            select s.endereco.distrito.id as distritoId, count(s) as total
            from SituacaoNoAlerta s
            where s.alerta.id = :alertaId and s.situacao in :situacoes
              and s.pessoa.tipoVulnerabilidade in :tipos
            group by s.endereco.distrito.id""")
    List<ContagemPorDistrito> contarPorTipoEDistrito(@Param("alertaId") Long alertaId,
                                                     @Param("situacoes") Collection<Situacao> situacoes,
                                                     @Param("tipos") Collection<TipoVulnerabilidade> tipos);

    @Query("""
            select s from SituacaoNoAlerta s
            where s.alerta.status = :statusAlerta
              and s.pessoa.id in (
                  select v.pessoa.id from VinculoResponsavel v
                  where v.usuario.id = :usuarioId and v.status = :statusVinculo)
            order by s.alerta.emitidoEm desc""")
    List<SituacaoNoAlerta> findAtivasDoResponsavel(@Param("usuarioId") Long usuarioId,
                                                   @Param("statusAlerta") StatusAlerta statusAlerta,
                                                   @Param("statusVinculo") StatusVinculo statusVinculo);
}