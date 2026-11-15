package br.com.fiap.beaconsync.alerta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
@Tag(name = "Alertas e mapa", description = "Emissão de alertas por distrito e mapa de risco")
public class AlertaController {

    private final AlertaService alertaService;
    private final MapaService mapaService;

    @PostMapping
    @PreAuthorize("hasRole('AGENTE_ORGAO')")
    @Operation(summary = "Emite um alerta para uma lista de distritos")
    public ResponseEntity<AlertaResponseDTO> emitir(@Valid @RequestBody AlertaRequestDTO request) {
        AlertaResponseDTO response = alertaService.emitir(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista paginada de alertas (filtro opcional por status)")
    public ResponseEntity<Page<AlertaResponseDTO>> listar(
            @RequestParam(value = "status", required = false) StatusAlerta status,
            @ParameterObject @PageableDefault(size = 10, sort = "emitidoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(alertaService.listar(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do alerta")
    public ResponseEntity<AlertaResponseDTO> buscarPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(alertaService.buscarPorId(id));
    }

    @PatchMapping("/{id}/encerrar")
    @PreAuthorize("hasRole('AGENTE_ORGAO')")
    @Operation(summary = "Encerra o alerta (só o órgão que emitiu)")
    public ResponseEntity<AlertaResponseDTO> encerrar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(alertaService.encerrar(id));
    }

    @GetMapping("/{id}/mapa")
    @Operation(summary = "Mapa por distrito: contagem e cor, sem nomes (qualquer usuário logado)")
    public ResponseEntity<MapaResponseDTO> mapa(@PathVariable("id") Long id) {
        return ResponseEntity.ok(mapaService.gerar(id));
    }

    @GetMapping("/{id}/distritos/{distritoId}/pessoas")
    @PreAuthorize("hasRole('AGENTE_ORGAO')")
    @Operation(summary = "Pessoas do distrito por prioridade (só com alerta ativo; acesso auditado)")
    public ResponseEntity<List<PessoaNoDistritoDTO>> pessoasDoDistrito(@PathVariable("id") Long id,
                                                                       @PathVariable("distritoId") Long distritoId) {
        return ResponseEntity.ok(alertaService.pessoasDoDistrito(id, distritoId));
    }
}