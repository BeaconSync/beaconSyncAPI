package br.com.fiap.beaconsync.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VinculoResponsavelRepository extends JpaRepository<VinculoResponsavel, Long> {

    Optional<VinculoResponsavel> findByPessoaIdAndUsuarioId(Long pessoaId, Long usuarioId);

    boolean existsByPessoaIdAndUsuarioIdAndStatus(Long pessoaId, Long usuarioId, StatusVinculo status);
}