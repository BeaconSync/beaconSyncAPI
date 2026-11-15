package br.com.fiap.beaconsync.alerta;

import java.time.LocalDateTime;

/** Situação de uma pessoa em um alerta, como o responsável vê no app. */
public record SituacaoResponseDTO(
        Long situacaoId,
        Long alertaId,
        String alertaTitulo,
        Long pessoaId,
        String pessoaNome,
        Long enderecoId,
        String enderecoApelido,
        Long distritoId,
        String distritoNome,
        Situacao situacao,
        String descricaoLivre,
        String respondidoPor,
        LocalDateTime respondidoEm
) {
    public SituacaoResponseDTO(SituacaoNoAlerta situacao) {
        this(
                situacao.getId(),
                situacao.getAlerta().getId(),
                situacao.getAlerta().getTitulo(),
                situacao.getPessoa().getId(),
                situacao.getPessoa().getNome(),
                situacao.getEndereco().getId(),
                situacao.getEndereco().getApelido(),
                situacao.getEndereco().getDistrito().getId(),
                situacao.getEndereco().getDistrito().getNome(),
                situacao.getSituacao(),
                situacao.getDescricaoLivre(),
                situacao.getRespondidoPor() != null ? situacao.getRespondidoPor().getNome() : null,
                situacao.getRespondidoEm()
        );
    }
}