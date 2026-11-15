package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.usuario.UsuarioResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Autenticação", description = "Login, renovação de sessão, logout e autocadastro")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    @Operation(summary = "Troca um refresh token por um novo par de tokens (sem Bearer)")
    public ResponseEntity<LoginResponseDTO> renovar(@Valid @RequestBody RefreshRequestDTO request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(refreshTokenService.renovar(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga a sessão do refresh token informado (sem Bearer)")
    public ResponseEntity<Void> encerrar(@Valid @RequestBody RefreshRequestDTO request) {
        refreshTokenService.encerrar(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Faz login e devolve o JWT para usar em Authorization: Bearer <token>")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(summary = "Autocadastro de RESPONSAVEL ou VOLUNTARIO")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }
}