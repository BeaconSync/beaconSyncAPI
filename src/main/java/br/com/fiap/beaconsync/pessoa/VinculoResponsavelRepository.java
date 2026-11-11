package br.com.fiap.beaconsync.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VinculoResponsavelRepository extends JpaRepository<VinculoResponsavel, Long> {

    Optional<VinculoResponsavel> findByPessoaIdAndUsuarioId(Long pessoaId, Long usuarioId);

    boolean existsByPessoaIdAndUsuarioIdAndStatus(Long pessoaId, Long usuarioId, StatusVinculo status);

    List<VinculoResponsavel> findByPessoaIdOrderByCriadoEmAsc(Long pessoaId);

    @Query("""
            select v from VinculoResponsavel v
            where v.status = :pendente
              and v.pessoa.id in (
                  select a.pessoa.id from VinculoResponsavel a
                  where a.usuario.id = :usuarioId and a.status = :ativo)
            order by v.criadoEm""")
    List<VinculoResponsavel> findPendentesParaAprovar(@Param("usuarioId") Long usuarioId,
                                                      @Param("pendente") StatusVinculo pendente,
                                                      @Param("ativo") StatusVinculo ativo);
}