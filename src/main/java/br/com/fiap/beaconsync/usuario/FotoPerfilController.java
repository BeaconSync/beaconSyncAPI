package br.com.fiap.beaconsync.usuario;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/usuarios/me/foto")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Dados do usuário logado")
public class FotoPerfilController {

    private final FotoPerfilService service;
    private final ImagemPerfil imagens;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia a foto da conta (campo arquivo, JPEG ou PNG até 2 MiB)")
    public ResponseEntity<UsuarioResponseDTO> salvar(@RequestPart("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.salvar(imagens.normalizar(arquivo)));
    }

    @GetMapping(produces = MediaType.IMAGE_JPEG_VALUE)
    @Operation(summary = "Devolve a foto da conta em JPEG (privada, sem cache)")
    public ResponseEntity<byte[]> consultar() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.IMAGE_JPEG)
                .body(service.consultar());
    }

    @DeleteMapping
    @Operation(summary = "Remove a foto da conta")
    public ResponseEntity<Void> excluir() {
        service.excluir();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}