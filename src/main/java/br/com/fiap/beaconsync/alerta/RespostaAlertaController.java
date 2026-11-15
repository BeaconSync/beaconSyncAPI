package br.com.fiap.beaconsync.alerta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESPONSAVEL')")
@Tag(name = "Respostas ao alerta", description = "O responsável confirma onde a pessoa está e se está segura")
public class RespostaAlertaController {

    private final AlertaService alertaService;

    @GetMapping("/ativos/meus")
    @Operation(summary = "Suas pessoas que estão em áreas com alerta ativo")
    public ResponseEntity<List<SituacaoResponseDTO>> minhasSituacoes() {
        return ResponseEntity.ok(alertaService.minhasSituacoesAtivas());
    }

    @PostMapping("/{alertaId}/respostas")
    @Operation(summary = "Responde: SEGURA, PRECISA_AJUDA ou SEM_CONTATO (pedido de ajuda vai para a triagem)")
    public ResponseEntity<SituacaoResponseDTO> responder(@PathVariable("alertaId") Long alertaId,
                                                         @Valid @RequestBody RespostaRequestDTO request) {
        return ResponseEntity.ok(alertaService.responder(alertaId, request));
    }
}