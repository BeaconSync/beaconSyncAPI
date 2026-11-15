package br.com.fiap.beaconsync.resgate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resgatistas")
@RequiredArgsConstructor
@Tag(name = "Resgatistas", description = "Disponibilidade de voluntários e profissionais")
public class ResgatistaController {

    private final ResgatistaService resgatistaService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('VOLUNTARIO', 'PROFISSIONAL')")
    @Operation(summary = "Seu perfil de resgatista")
    public ResponseEntity<ResgatistaResponseDTO> meuPerfil() {
        return ResponseEntity.ok(resgatistaService.meuPerfil());
    }

    @PatchMapping("/me/disponibilidade")
    @PreAuthorize("hasAnyRole('VOLUNTARIO', 'PROFISSIONAL')")
    @Operation(summary = "Informa se você está disponível para ajudar (e em qual distrito)")
    public ResponseEntity<ResgatistaResponseDTO> atualizarDisponibilidade(
            @Valid @RequestBody DisponibilidadeRequestDTO request) {
        return ResponseEntity.ok(resgatistaService.atualizarDisponibilidade(request));
    }

    @GetMapping("/disponiveis")
    @PreAuthorize("hasRole('AGENTE_ORGAO')")
    @Operation(summary = "Voluntários disponíveis e profissionais do seu órgão (filtro opcional por distrito)")
    public ResponseEntity<List<ResgatistaResponseDTO>> disponiveis(
            @RequestParam(value = "distritoId", required = false) Long distritoId) {
        return ResponseEntity.ok(resgatistaService.disponiveis(distritoId));
    }
}