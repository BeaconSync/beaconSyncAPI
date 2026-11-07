package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "BS_HISTORICO_OCORRENCIA")
@Getter
@Setter
@NoArgsConstructor
public class HistoricoOcorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private Ocorrencia ocorrencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 15)
    private StatusOcorrencia statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 15)
    private StatusOcorrencia statusNovo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alterado_por_id")
    private Usuario alteradoPor;

    @Column(name = "alterado_em", nullable = false)
    private LocalDateTime alteradoEm;

    @Column(length = 500)
    private String observacao;

    @PrePersist
    void aoCriar() {
        if (alteradoEm == null) {
            alteradoEm = LocalDateTime.now();
        }
    }
}