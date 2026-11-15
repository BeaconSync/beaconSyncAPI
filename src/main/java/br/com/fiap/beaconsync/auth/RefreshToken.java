package br.com.fiap.beaconsync.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "BS_REFRESH_TOKEN")
@Getter @Setter @NoArgsConstructor
public class RefreshToken {
    @Id @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoRefresh sessao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;
}