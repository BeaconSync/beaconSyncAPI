package br.com.fiap.beaconsync.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilDTO(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank
        @Pattern(
                regexp = "[0-9]{10,11}",
                message = "Telefone com DDD, só números (10 ou 11 dígitos)")
        String telefone,
        Papel papel) {
    public AtualizarPerfilDTO(String nome, String telefone) {
        this(nome, telefone, null);
    }
}