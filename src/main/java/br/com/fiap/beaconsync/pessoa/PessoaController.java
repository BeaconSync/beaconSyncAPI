package br.com.fiap.beaconsync.pessoa;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/pessoas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESPONSAVEL')")
@Tag(name = "Pessoas vulneráveis", description = "Cadastro prévio feito pelos responsáveis (família e amigos)")
public class PessoaController {

    private final PessoaService pessoaService;

    @PostMapping
    @Operation(summary = "Cadastra a pessoa com o primeiro endereço (409 se o CPF já existir)")
    public ResponseEntity<PessoaResponseDTO> cadastrar(@Valid @RequestBody PessoaRequestDTO request) {
        PessoaResponseDTO response = pessoaService.cadastrar(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista paginada das pessoas em que você é responsável")
    public ResponseEntity<Page<PessoaResponseDTO>> listar(
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(pessoaService.listarMinhas(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da pessoa (só para responsáveis vinculados)")
    public ResponseEntity<PessoaResponseDTO> buscarPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(pessoaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados da pessoa (o CPF não muda)")
    public ResponseEntity<PessoaResponseDTO> atualizar(@PathVariable("id") Long id,
                                                       @Valid @RequestBody PessoaUpdateDTO request) {
        return ResponseEntity.ok(pessoaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove o cadastro (revoga o consentimento)")
    public ResponseEntity<Void> excluir(@PathVariable("id") Long id) {
        pessoaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}