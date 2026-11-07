package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.distrito.Distrito;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "BS_ENDERECO")
@Getter
@Setter
@NoArgsConstructor
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private PessoaVulneravel pessoa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distrito_id", nullable = false)
    private Distrito distrito;

    /** Ex.: "Casa", "Casa da filha", "Centro-dia". */
    @Column(nullable = false, length = 60)
    private String apelido;

    @Column(nullable = false, length = 150)
    private String logradouro;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(length = 60)
    private String complemento;

    /** Só dígitos (8). */
    @Column(nullable = false, length = 8)
    private String cep;
}