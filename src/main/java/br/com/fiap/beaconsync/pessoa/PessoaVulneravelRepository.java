package br.com.fiap.beaconsync.pessoa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PessoaVulneravelRepository extends JpaRepository<PessoaVulneravel, Long> {

    Optional<PessoaVulneravel> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    /** Pessoas em que o usuário é responsável com o status informado (normalmente ATIVO). */
    @Query("select p from PessoaVulneravel p join p.vinculos v "
            + "where v.usuario.id = :usuarioId and v.status = :status")
    Page<PessoaVulneravel> findVinculadas(@Param("usuarioId") Long usuarioId,
                                          @Param("status") StatusVinculo status,
                                          Pageable pageable);

    /** Pessoas cujo endereço ATUAL está em um dos distritos (quem entra no alerta). */
    @Query("select p from PessoaVulneravel p join fetch p.enderecoAtual e "
            + "where e.distrito.id in :distritoIds")
    List<PessoaVulneravel> findComEnderecoAtualNosDistritos(@Param("distritoIds") Collection<Long> distritoIds);

    /**
     * Exclusão direta no banco: o ON DELETE CASCADE das FKs remove endereços,
     * vínculos e situações em alertas da pessoa num único comando.
     */
    @Modifying
    @Query("delete from PessoaVulneravel p where p.id = :id")
    int excluirPorId(@Param("id") Long id);
}