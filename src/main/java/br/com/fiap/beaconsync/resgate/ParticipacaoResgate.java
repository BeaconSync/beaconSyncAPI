package br.com.fiap.beaconsync.resgate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Equipe do resgate: N:N entre ocorrência e resgatista, com função e horário. */
@Entity
@Table(name = "BS_PARTICIPACAO_RESGATE",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ocorrencia_id", "resgatista_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ParticipacaoResgate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private Ocorrencia ocorrencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resgatista_id", nullable = false)
    private Resgatista resgatista;

    /** Ex.: "condutor do barco", "socorrista". */
    @Column(length = 60)
    private String funcao;

    @Column(name = "entrou_em", nullable = false)
    private LocalDateTime entrouEm;

    @PrePersist
    void aoCriar() {
        if (entrouEm == null) {
            entrouEm = LocalDateTime.now();
        }
    }
}