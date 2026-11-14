package br.com.fiap.beaconsync.notificacao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "Avisos de alerta e de andamento do resgate, lidos pelo app")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    @Operation(summary = "Seus avisos, do mais recente para o mais antigo")
    public ResponseEntity<Page<NotificacaoResponseDTO>> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(notificacaoService.listarMinhas(pageable));
    }

    @PatchMapping("/{id}/lida")
    @Operation(summary = "Marca um aviso como lido")
    public ResponseEntity<NotificacaoResponseDTO> marcarComoLida(@PathVariable("id") Long id) {
        return ResponseEntity.ok(notificacaoService.marcarComoLida(id));
    }
}