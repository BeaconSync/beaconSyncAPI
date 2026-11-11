package br.com.fiap.beaconsync.distrito;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lista os 96 distritos de SP (público: é dado de referência para o cadastro de endereço). */
@RestController
@RequestMapping("/api/v1/distritos")
@RequiredArgsConstructor
@Tag(name = "Distritos", description = "Distritos oficiais de São Paulo usados no alerta e no mapa")
public class DistritoController {

    private final DistritoRepository repository;

    @GetMapping
    @Operation(summary = "Lista paginada de distritos (padrão: 100 por página, ordem alfabética)")
    public ResponseEntity<Page<DistritoResponseDTO>> listar(
            @ParameterObject @PageableDefault(size = 100, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable).map(DistritoResponseDTO::new));
    }
}