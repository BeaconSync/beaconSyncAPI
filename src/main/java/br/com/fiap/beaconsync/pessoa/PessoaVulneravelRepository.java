package br.com.fiap.beaconsync.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PessoaVulneravelRepository extends JpaRepository<PessoaVulneravel, Long> {

    Optional<PessoaVulneravel> findByCpf(String cpf);

    boolean existsByCpf(String cpf);
}