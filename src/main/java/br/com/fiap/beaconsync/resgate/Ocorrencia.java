package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.alerta.SituacaoNoAlerta;
import br.com.fiap.beaconsync.orgao.Orgao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Pedido de resgate acompanhado por um órgão até o fim. */
@Entity
@Table(name = "BS_OCORRENCIA")
@Getter
@Setter
@NoArgsConstructor
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "situacao_id", nullable = false, unique = true)
    private SituacaoNoAlerta situacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orgao_id", nullable = false)
    private Orgao orgao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusOcorrencia status = StatusOcorrencia.SOLICITADA;

    /** Ordem sugerida pelo sistema (0 a 100). */
    @Column(name = "score_prioridade", nullable = false, precision = 5, scale = 2)
    private BigDecimal scorePrioridade = BigDecimal.ZERO;

    /** Ordem definida pelo agente; quando preenchida, vence o score. */
    @Column(name = "prioridade_manual")
    private Integer prioridadeManual;

    @OneToMany(mappedBy = "ocorrencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParticipacaoResgate> equipe = new ArrayList<>();

    @OneToMany(mappedBy = "ocorrencia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("alteradoEm ASC")
    private List<HistoricoOcorrencia> historico = new ArrayList<>();

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @PrePersist
    void aoCriar() {
        if (criadaEm == null) {
            criadaEm = LocalDateTime.now();
        }
    }
}