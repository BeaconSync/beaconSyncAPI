package br.com.fiap.beaconsync.resgate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrioridadeRequestDTO(
        @Min(value = 1, message = "A prioridade manual começa em 1")
        @Max(value = 9999, message = "A prioridade manual vai até 9999")
        Integer prioridadeManual,

        @NotBlank(message = "Explique o motivo da mudança de ordem (fica na auditoria)")
        @Size(max = 300, message = "A justificativa deve ter no máximo 300 caracteres")
        String justificativa
) {}