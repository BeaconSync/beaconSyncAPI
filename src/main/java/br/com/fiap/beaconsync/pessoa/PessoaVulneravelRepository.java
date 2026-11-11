package br.com.fiap.beaconsync.pessoa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PessoaVulneravelRepository extends JpaRepository<PessoaVulneravel, Long> {

    Optional<PessoaVulneravel> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    @Query("select p from PessoaVulneravel p join p.vinculos v "
            + "where v.usuario.id = :usuarioId and v.status = :status")
    Page<PessoaVulneravel> findVinculadas(@Param("usuarioId") Long usuarioId,
                                          @Param("status") StatusVinculo status,
                                          Pageable pageable);

    @Modifying
    @Query("delete from PessoaVulneravel p where p.id = :id")
    int excluirPorId(@Param("id") Long id);
}