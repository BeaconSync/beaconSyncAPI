package br.com.fiap.beaconsync.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Erro 400 de Bean Validation, com a lista de campos inválidos. */
public record ErroValidacao(
        LocalDateTime timestamp,
        Integer status,
        String erro,
        String path,
        List<CampoInvalido> campos
) {
    public record CampoInvalido(String campo, String mensagem) {}
}