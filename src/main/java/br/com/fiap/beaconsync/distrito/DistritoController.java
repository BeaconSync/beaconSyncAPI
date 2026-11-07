package br.com.fiap.beaconsync.distrito;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lista os 96 distritos de SP. Na etapa 1 serve também como teste de que
 * a API conectou no Oracle e o Flyway rodou.
 */
@RestController
@RequestMapping("/api/v1/distritos")
@Tag(name = "Distritos", description = "Distritos oficiais de São Paulo usados no alerta e no mapa")
public class DistritoController {

    private final DistritoRepository repository;

    public DistritoController(DistritoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Lista paginada de distritos (padrão: 100 por página, ordem alfabética)")
    public Page<DistritoResponse> listar(@PageableDefault(size = 100, sort = "nome") Pageable pageable) {
        return repository.findAll(pageable).map(DistritoResponse::de);
    }
}