package br.com.fiap.beaconsync.resgate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResgatistaRepository extends JpaRepository<Resgatista, Long> {

    Optional<Resgatista> findByUsuarioId(Long usuarioId);
}