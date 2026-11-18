package br.com.fiap.beaconsync.pessoa;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface MinhasSolicitacoesRepository extends Repository<VinculoResponsavel, Long> {
    interface Pedido {
        Long getId();
        String getParentesco();
        String getStatus();
        LocalDateTime getCriadaEm();
        LocalDateTime getDecididaEm();
    }

    // O evento SOLICITAR_VINCULO distingue pedido de vínculo automático.
    // O maior ID identifica a última tentativa mesmo com timestamps iguais.
    String ORIGEM = """
            FROM BS_VINCULO_RESPONSAVEL v
            JOIN BS_AUDIT_LOG s ON s.entidade_id = v.id
              AND s.entidade = 'VINCULO_RESPONSAVEL'
              AND s.acao = 'SOLICITAR_VINCULO'
              AND s.usuario_id = v.usuario_id
            WHERE v.usuario_id = :usuarioId
              AND NOT EXISTS (
                SELECT 1 FROM BS_AUDIT_LOG nova
                WHERE nova.entidade = s.entidade
                  AND nova.entidade_id = s.entidade_id
                  AND nova.acao = s.acao
                  AND nova.usuario_id = s.usuario_id
                  AND nova.id > s.id
              )
            """;

    @Query(value = """
            SELECT v.id AS "id", v.parentesco AS "parentesco", v.status AS "status",
                   s.criado_em AS "criadaEm",
                   (SELECT MAX(d.criado_em) FROM BS_AUDIT_LOG d
                    WHERE d.entidade = 'VINCULO_RESPONSAVEL'
                      AND d.entidade_id = v.id AND d.id > s.id
                      AND ((v.status = 'ATIVO' AND d.acao = 'APROVAR_VINCULO')
                        OR (v.status = 'RECUSADO' AND d.acao = 'RECUSAR_VINCULO'))
                   ) AS "decididaEm"
            """ + ORIGEM + " ORDER BY s.criado_em DESC, v.id DESC",
            countQuery = "SELECT COUNT(*) " + ORIGEM, nativeQuery = true)
    Page<Pedido> consultar(@Param("usuarioId") Long usuarioId, Pageable pageable);
}
