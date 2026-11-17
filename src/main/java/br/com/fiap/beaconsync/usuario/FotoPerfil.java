package br.com.fiap.beaconsync.usuario;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "BS_USUARIO_FOTO")
@Getter
@Setter
@NoArgsConstructor
public class FotoPerfil {
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Lob
    @Column(nullable = false)
    private byte[] conteudo;
}