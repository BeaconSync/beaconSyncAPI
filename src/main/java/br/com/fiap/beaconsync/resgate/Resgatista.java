package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.distrito.Distrito;
import br.com.fiap.beaconsync.orgao.Orgao;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

/** Perfil de resgate de um usuário: profissional (ligado a um órgão) ou voluntário. */
@Entity
@Table(name = "BS_RESGATISTA")
@Getter
@Setter
@NoArgsConstructor
public class Resgatista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoResgatista tipo;

    /** Ex.: "barco, conhece o bairro", "socorrista". */
    @Column(length = 300)
    private String habilidades;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private Boolean disponivel = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orgao_id")
    private Orgao orgao;

    /** Região onde o voluntário costuma ajudar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distrito_base_id")
    private Distrito distritoBase;
}