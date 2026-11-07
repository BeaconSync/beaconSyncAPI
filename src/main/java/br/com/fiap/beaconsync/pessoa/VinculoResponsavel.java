package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Liga um responsável (usuário) a uma pessoa vulnerável. */
@Entity
@Table(name = "BS_VINCULO_RESPONSAVEL",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pessoa_id", "usuario_id"}))
@Getter
@Setter
@NoArgsConstructor
public class VinculoResponsavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private PessoaVulneravel pessoa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Ex.: "filha", "neto", "vizinho". */
    @Column(nullable = false, length = 40)
    private String parentesco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusVinculo status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}