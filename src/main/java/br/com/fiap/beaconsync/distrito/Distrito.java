package br.com.fiap.beaconsync.distrito;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Um dos 96 distritos oficiais de São Paulo (carregados pela migration V2). */
@Entity
@Table(name = "BS_DISTRITO")
@Getter
@Setter
@NoArgsConstructor
public class Distrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer codigo;

    @Column(nullable = false, unique = true, length = 60)
    private String nome;

    @Column(nullable = false, length = 60)
    private String subprefeitura;
}