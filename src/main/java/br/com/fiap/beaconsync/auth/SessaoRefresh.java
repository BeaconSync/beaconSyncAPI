package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "BS_SESSAO_REFRESH")
@Getter @Setter @NoArgsConstructor
public class SessaoRefresh {
    @Id @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "revogada_em")
    private LocalDateTime revogadaEm;
}