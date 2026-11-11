package br.com.fiap.beaconsync.orgao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrgaoRepository extends JpaRepository<Orgao, Long> {

    Optional<Orgao> findFirstByTipo(TipoOrgao tipo);
}