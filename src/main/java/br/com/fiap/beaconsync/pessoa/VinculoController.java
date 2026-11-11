package br.com.fiap.beaconsync.pessoa;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESPONSAVEL')")
@Tag(name = "Vínculos", description = "Vários responsáveis para a mesma pessoa, sem duplicar o cadastro")
public class VinculoController {

    private final VinculoService vinculoService;

    @PostMapping("/pessoas/{pessoaId}/vinculos")
    @Operation(summary = "Pede para ser responsável por uma pessoa já cadastrada")
    public ResponseEntity<VinculoResponseDTO> solicitar(@PathVariable("pessoaId") Long pessoaId,
                                                        @Valid @RequestBody VinculoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vinculoService.solicitar(pessoaId, request));
    }

    @GetMapping("/pessoas/{pessoaId}/vinculos")
    @Operation(summary = "Lista os responsáveis da pessoa e os pedidos de vínculo")
    public ResponseEntity<List<VinculoResponseDTO>> listarDaPessoa(@PathVariable("pessoaId") Long pessoaId) {
        return ResponseEntity.ok(vinculoService.listarDaPessoa(pessoaId));
    }

    @GetMapping("/vinculos/pendentes")
    @Operation(summary = "Pedidos de vínculo que você pode aprovar")
    public ResponseEntity<List<VinculoResponseDTO>> pendentes() {
        return ResponseEntity.ok(vinculoService.pendentesParaAprovar());
    }

    @PatchMapping("/vinculos/{id}")
    @Operation(summary = "Aprova ou recusa um pedido de vínculo")
    public ResponseEntity<VinculoResponseDTO> decidir(@PathVariable("id") Long id,
                                                      @Valid @RequestBody VinculoDecisaoDTO request) {
        return ResponseEntity.ok(vinculoService.decidir(id, request));
    }
}