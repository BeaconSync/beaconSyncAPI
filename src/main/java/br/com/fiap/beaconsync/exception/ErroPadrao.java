package br.com.fiap.beaconsync.exception;

import java.time.LocalDateTime;

public record ErroPadrao(
        LocalDateTime timestamp,
        Integer status,
        String erro,
        String mensagem,
        String path
) {}