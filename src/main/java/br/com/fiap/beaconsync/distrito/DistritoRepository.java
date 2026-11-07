package br.com.fiap.beaconsync.distrito;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistritoRepository extends JpaRepository<Distrito, Long> {

    Optional<Distrito> findByNomeIgnoreCase(String nome);
}