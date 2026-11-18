package br.com.fiap.beaconsync.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErroValidacao(
        LocalDateTime timestamp,
        Integer status,
        String erro,
        String path,
        List<CampoInvalido> campos
) {
    public record CampoInvalido(String campo, String mensagem) {}
}