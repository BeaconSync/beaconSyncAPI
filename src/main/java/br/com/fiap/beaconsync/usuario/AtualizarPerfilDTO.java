package br.com.fiap.beaconsync.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record AtualizarPerfilDTO(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank
        @Pattern(
                regexp = "[0-9]{10,11}",
                message = "Telefone com DDD, só números (10 ou 11 dígitos)")
        String telefone,
        Papel papel,
        @Email @Size(min = 5, max = 160) String email) {
    public AtualizarPerfilDTO {
        if (email != null) email = email.trim().toLowerCase(Locale.ROOT);
    }

    public AtualizarPerfilDTO(String nome, String telefone, Papel papel) {
        this(nome, telefone, papel, null);
    }

    public AtualizarPerfilDTO(String nome, String telefone) {
        this(nome, telefone, null, null);
    }
}
