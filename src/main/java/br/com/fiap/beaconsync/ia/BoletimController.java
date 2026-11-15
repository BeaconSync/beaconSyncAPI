package br.com.fiap.beaconsync.ia;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
@Tag(name = "Boletim (IA)", description = "Resumo do alerta gerado por IA a partir dos números do mapa e da fila")
public class BoletimController {

    private final BoletimService boletimService;

    @GetMapping("/{id}/boletim")
    @PreAuthorize("hasRole('AGENTE_ORGAO')")
    @Operation(summary = "Boletim do alerta (texto da IA + números brutos para conferência)")
    public ResponseEntity<BoletimResponseDTO> boletim(@PathVariable("id") Long id) {
        return ResponseEntity.ok(boletimService.gerar(id));
    }
}