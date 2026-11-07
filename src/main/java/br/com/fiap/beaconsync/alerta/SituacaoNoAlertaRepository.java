package br.com.fiap.beaconsync.alerta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SituacaoNoAlertaRepository extends JpaRepository<SituacaoNoAlerta, Long> {

    Optional<SituacaoNoAlerta> findByAlertaIdAndPessoaId(Long alertaId, Long pessoaId);
}