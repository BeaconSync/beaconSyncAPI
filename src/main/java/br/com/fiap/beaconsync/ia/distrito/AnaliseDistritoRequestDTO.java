package br.com.fiap.beaconsync.ia.distrito;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnaliseDistritoRequestDTO(
        @NotBlank @Size(min = 5, max = 500) String pergunta) {
    public AnaliseDistritoRequestDTO {
        if (pergunta != null) pergunta = pergunta.strip();
    }
}