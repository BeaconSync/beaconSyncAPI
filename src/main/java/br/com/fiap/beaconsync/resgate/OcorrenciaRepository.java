package br.com.fiap.beaconsync.resgate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    Optional<Ocorrencia> findBySituacaoId(Long situacaoId);

    List<Ocorrencia> findByOrgaoIdAndStatusIn(Long orgaoId, Collection<StatusOcorrencia> status);

    @Query("select p.ocorrencia from ParticipacaoResgate p "
            + "where p.resgatista.usuario.id = :usuarioId order by p.entrouEm desc")
    List<Ocorrencia> findDoResgatista(@Param("usuarioId") Long usuarioId);

    @Query("""
            select o.situacao.endereco.distrito.id as distritoId, o.status as status, count(o) as total
            from Ocorrencia o
            where o.situacao.alerta.id = :alertaId
            group by o.situacao.endereco.distrito.id, o.status""")
    List<ContagemOcorrencia> contarPorDistrito(@Param("alertaId") Long alertaId);
}