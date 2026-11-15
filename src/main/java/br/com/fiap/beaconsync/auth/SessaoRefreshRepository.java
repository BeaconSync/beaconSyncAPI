package br.com.fiap.beaconsync.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SessaoRefreshRepository extends JpaRepository<SessaoRefresh, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoRefresh s where s.id = :id")
    Optional<SessaoRefresh> buscarComBloqueio(@Param("id") String id);
}