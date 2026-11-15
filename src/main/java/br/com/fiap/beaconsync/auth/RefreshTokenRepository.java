package br.com.fiap.beaconsync.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    @Query("select t.sessao.id from RefreshToken t where t.tokenHash = :hash")
    Optional<String> buscarSessaoId(@Param("hash") String hash);
}