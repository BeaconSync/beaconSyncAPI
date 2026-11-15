package br.com.fiap.beaconsync.resgate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ocorrencias")
@RequiredArgsConstructor
@Tag(name = "Resgates", description = "Fila do órgão, equipe e etapas de cada resgate")
public class OcorrenciaController {

    private final ResgateService resgateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENTE_ORGAO', 'PROFISSIONAL')")
    @Operation(summary = "Fila do seu órgão, na ordem de prioridade (padrão: solicitadas e em andamento)")
    public ResponseEntity<Page<OcorrenciaResumoDTO>> fila(
            @RequestParam(value = "status", required = false) StatusOcorrencia status,
            @RequestParam(value = "alertaId", required = false) Long alertaId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return ResponseEntity.ok(resgateService.fila(status, alertaId, pagina));
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasAnyRole('PROFISSIONAL', 'VOLUNTARIO')")
    @Operation(summary = "Resgates em que você foi escalado")
    public ResponseEntity<List<OcorrenciaResumoDTO>> minhas() {
        return ResponseEntity.ok(resgateService.minhas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENTE_ORGAO', 'PROFISSIONAL', 'VOLUNTARIO')")
    @Operation(summary = "Detalhe do resgate: endereço, necessidades, contatos, equipe e histórico")
    public ResponseEntity<OcorrenciaDetalheDTO> detalhe(@PathVariable("id") Long id) {
        return ResponseEntity.ok(resgateService.detalhe(id));
    }

    @PatchMapping("/{id}/prioridade")
    @PreAuthorize("hasRole('AGENTE_ORGAO')")
    @Operation(summary = "Muda a ordem sugerida (1 = primeiro; null volta para a sugestão)")
    public ResponseEntity<OcorrenciaResumoDTO> definirPrioridade(@PathVariable("id") Long id,
                                                                 @Valid @RequestBody PrioridadeRequestDTO request) {
        return ResponseEntity.ok(resgateService.definirPrioridade(id, request));
    }

    @PostMapping("/{id}/equipe")
    @PreAuthorize("hasRole('AGENTE_ORGAO')")
    @Operation(summary = "Escala um profissional ou voluntário disponível para o resgate")
    public ResponseEntity<OcorrenciaDetalheDTO> escalar(@PathVariable("id") Long id,
                                                        @Valid @RequestBody EscalaRequestDTO request) {
        return ResponseEntity.ok(resgateService.escalar(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENTE_ORGAO', 'PROFISSIONAL')")
    @Operation(summary = "Avança a etapa: EM_ANDAMENTO, CONCLUIDA ou CANCELADA (a família é avisada)")
    public ResponseEntity<OcorrenciaDetalheDTO> atualizarStatus(@PathVariable("id") Long id,
                                                                @Valid @RequestBody StatusOcorrenciaRequestDTO request) {
        return ResponseEntity.ok(resgateService.atualizarStatus(id, request));
    }
}