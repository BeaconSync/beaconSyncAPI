package br.com.fiap.beaconsync.alerta;

import br.com.fiap.beaconsync.pessoa.Endereco;
import br.com.fiap.beaconsync.pessoa.PessoaVulneravel;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

import java.time.LocalDateTime;

/**
 * Situação de uma pessoa em um alerta: uma linha por (alerta, pessoa).
 * Vários responsáveis respondem sobre a mesma linha (vale a mais recente),
 * por isso o pedido de resgate nunca duplica.
 */
@Entity
@Table(name = "BS_SITUACAO_ALERTA",
        uniqueConstraints = @UniqueConstraint(columnNames = {"alerta_id", "pessoa_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SituacaoNoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private PessoaVulneravel pessoa;

    /** Endereço onde a pessoa está neste alerta (cópia do endereço atual). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco endereco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao = Situacao.PENDENTE;

    /** Relato livre do responsável (vai para a triagem, sem nome/CPF/endereço). */
    @Column(name = "descricao_livre", length = 2000)
    private String descricaoLivre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondido_por_id")
    private Usuario respondidoPor;

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    // ---- Resultado da triagem (IA ou regra base) ----

    /** 1 (baixa) a 5 (crítica). */
    private Integer gravidade;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "risco_imediato")
    private Boolean riscoImediato;

    @Column(length = 500)
    private String necessidades;

    @Column(name = "resumo_equipe", length = 1000)
    private String resumoEquipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_triagem", length = 10)
    private OrigemTriagem origemTriagem;

    @Column(name = "triado_em")
    private LocalDateTime triadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    /** Controle de concorrência otimista (dois responsáveis respondendo juntos). */
    @Version
    @Column(nullable = false)
    private Long versao;

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}