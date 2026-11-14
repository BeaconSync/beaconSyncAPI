package br.com.fiap.beaconsync.notificacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByUsuarioId(Long usuarioId, Pageable pageable);

    Optional<Notificacao> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);
}