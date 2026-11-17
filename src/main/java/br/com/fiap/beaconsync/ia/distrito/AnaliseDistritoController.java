package br.com.fiap.beaconsync.ia.distrito;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
@Tag(name = "Análise de distrito", description = "IA Python com contagens consultadas pelo Java")
public class AnaliseDistritoController {
    private final AnaliseDistritoService service;

    @PostMapping("/{alertaId}/distritos/{distritoId}/analise")
    @PreAuthorize("hasAnyRole('AGENTE_ORGAO', 'VOLUNTARIO')")
    @Operation(summary = "Explica as contagens do distrito usando a IA Python")
    public ResponseEntity<IaAnaliseResponse> analisar(
            @PathVariable("alertaId") Long alertaId,
            @PathVariable("distritoId") Long distritoId,
            @Valid @RequestBody AnaliseDistritoRequestDTO request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.analisar(alertaId, distritoId, request.pergunta()));
    }
}