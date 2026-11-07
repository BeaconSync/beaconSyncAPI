package br.com.fiap.beaconsync.pessoa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pessoa que precisa de ajuda para sair de casa. Tem um único registro
 * (CPF único), vários endereços, um endereço atual e vários responsáveis.
 */
@Entity
@Table(name = "BS_PESSOA_VULNERAVEL")
@Getter
@Setter
@NoArgsConstructor
public class PessoaVulneravel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    /** Só dígitos (11). Chave do registro único. */
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vulnerabilidade", nullable = false, length = 30)
    private TipoVulnerabilidade tipoVulnerabilidade;

    /** Ex.: "maca, cilindro de oxigênio". */
    @Column(name = "necessidades_remocao", length = 500)
    private String necessidadesRemocao;

    @Column(name = "consentimento_em", nullable = false)
    private LocalDateTime consentimentoEm;

    @OneToMany(mappedBy = "pessoa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Endereco> enderecos = new ArrayList<>();

    /** Endereço onde a pessoa está agora: é o usado no mapa e no resgate. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_atual_id")
    private Endereco enderecoAtual;

    @OneToMany(mappedBy = "pessoa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VinculoResponsavel> vinculos = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public void adicionarEndereco(Endereco endereco) {
        endereco.setPessoa(this);
        enderecos.add(endereco);
    }

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }
}