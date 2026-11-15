package br.com.fiap.beaconsync.ia.distrito;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IaAnaliseRequest(
        @JsonProperty("alerta_id") String alertaId,
        @JsonProperty("distrito_id") String distritoId,
        @JsonProperty("distrito_nome") String distritoNome,
        @JsonProperty("atualizado_em") String atualizadoEm,
        Situacoes situacoes,
        String pergunta) {

    public record Situacoes(@JsonProperty("precisa_ajuda") long precisaAjuda,
                            @JsonProperty("sem_contato") long semContato,
                            @JsonProperty("pendente_confirmacao") long pendenteConfirmacao,
                            long segura, long resgatada) {
    }
}