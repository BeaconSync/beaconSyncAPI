package br.com.fiap.beaconsync.notificacao;

import java.time.LocalDateTime;

public record NotificacaoResponseDTO(
        Long id,
        String titulo,
        String mensagem,
        boolean lida,
        LocalDateTime criadaEm
) {
    public NotificacaoResponseDTO(Notificacao notificacao) {
        this(
                notificacao.getId(),
                notificacao.getTitulo(),
                notificacao.getMensagem(),
                Boolean.TRUE.equals(notificacao.getLida()),
                notificacao.getCriadaEm()
        );
    }
}