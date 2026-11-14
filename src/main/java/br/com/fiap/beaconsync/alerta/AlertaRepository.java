package br.com.fiap.beaconsync.alerta;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    Page<Alerta> findByStatus(StatusAlerta status, Pageable pageable);

    List<Alerta> findByStatus(StatusAlerta status);
}