package br.com.fiap.beaconsync.exception;

import java.time.LocalDateTime;

public record ErroConflito(
        LocalDateTime timestamp,
        Integer status,
        String erro,
        String mensagem,
        String path,
        Long recursoId
) {}