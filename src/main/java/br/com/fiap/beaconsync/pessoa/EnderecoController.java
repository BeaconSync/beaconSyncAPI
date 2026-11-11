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
@RequestMapping("/api/v1/pessoas/{pessoaId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESPONSAVEL')")
@Tag(name = "Endereços", description = "Endereços da pessoa e onde ela está agora")
public class EnderecoController {

    private final EnderecoService enderecoService;

    @GetMapping("/enderecos")
    @Operation(summary = "Lista os endereços da pessoa (o atual vem com atual=true)")
    public ResponseEntity<List<EnderecoResponseDTO>> listar(@PathVariable("pessoaId") Long pessoaId) {
        return ResponseEntity.ok(enderecoService.listar(pessoaId));
    }

    @PostMapping("/enderecos")
    @Operation(summary = "Adiciona um endereço (ex.: casa da filha, centro-dia)")
    public ResponseEntity<EnderecoResponseDTO> adicionar(@PathVariable("pessoaId") Long pessoaId,
                                                         @Valid @RequestBody EnderecoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enderecoService.adicionar(pessoaId, request));
    }

    @PutMapping("/endereco-atual")
    @Operation(summary = "Define onde a pessoa está agora (endereço usado no mapa e no resgate)")
    public ResponseEntity<PessoaResponseDTO> definirAtual(@PathVariable("pessoaId") Long pessoaId,
                                                          @Valid @RequestBody EnderecoAtualRequestDTO request) {
        return ResponseEntity.ok(enderecoService.definirAtual(pessoaId, request));
    }

    @DeleteMapping("/enderecos/{enderecoId}")
    @Operation(summary = "Remove um endereço (não pode ser o atual)")
    public ResponseEntity<Void> remover(@PathVariable("pessoaId") Long pessoaId,
                                        @PathVariable("enderecoId") Long enderecoId) {
        enderecoService.remover(pessoaId, enderecoId);
        return ResponseEntity.noContent().build();
    }
}