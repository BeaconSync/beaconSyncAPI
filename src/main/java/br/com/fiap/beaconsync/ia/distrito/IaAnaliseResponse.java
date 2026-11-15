package br.com.fiap.beaconsync.ia.distrito;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IaAnaliseResponse(
        @JsonProperty("id_analise") String idAnalise,
        String modo, String status, String modelo,
        @JsonProperty("alerta_id") String alertaId,
        @JsonProperty("distrito_id") String distritoId,
        @JsonProperty("dados_atualizados_em") String dadosAtualizadosEm,
        @JsonProperty("gerado_em") String geradoEm,
        @JsonProperty("total_cadastrado") Long totalCadastrado,
        @JsonProperty("total_acompanhamento") Long totalAcompanhamento,
        @JsonProperty("boletim_dados") String boletimDados,
        String analise,
        @JsonProperty("fontes_recuperadas") List<Fonte> fontesRecuperadas,
        List<String> avisos) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fonte(String id, String titulo, String origem, String trecho, Double similaridade) {}
}