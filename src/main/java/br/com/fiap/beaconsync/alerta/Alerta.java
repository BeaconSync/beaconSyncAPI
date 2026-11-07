package br.com.fiap.beaconsync.alerta;

import br.com.fiap.beaconsync.distrito.Distrito;
import br.com.fiap.beaconsync.orgao.Orgao;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** Alerta de enchente emitido por um órgão para uma lista de distritos. */
@Entity
@Table(name = "BS_ALERTA")
@Getter
@Setter
@NoArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orgao_id", nullable = false)
    private Orgao orgao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emitido_por_id", nullable = false)
    private Usuario emitidoPor;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusAlerta status = StatusAlerta.ATIVO;

    @ManyToMany
    @JoinTable(name = "BS_ALERTA_DISTRITO",
            joinColumns = @JoinColumn(name = "alerta_id"),
            inverseJoinColumns = @JoinColumn(name = "distrito_id"))
    private Set<Distrito> distritos = new HashSet<>();

    @Column(name = "emitido_em", nullable = false, updatable = false)
    private LocalDateTime emitidoEm;

    @Column(name = "encerrado_em")
    private LocalDateTime encerradoEm;

    @PrePersist
    void aoCriar() {
        if (emitidoEm == null) {
            emitidoEm = LocalDateTime.now();
        }
    }
}