package br.com.fiap.beaconsync.alerta;

/** Situação da pessoa vulnerável durante um alerta. */
public enum Situacao {
    PENDENTE,
    SEGURA,
    PRECISA_AJUDA,
    SEM_CONTATO,
    RESGATADA;

    /** Entra na contagem do mapa enquanto não está segura nem resgatada. */
    public boolean contaNoMapa() {
        return this == PENDENTE || this == PRECISA_AJUDA || this == SEM_CONTATO;
    }

    /** Gera pedido de resgate para o órgão. */
    public boolean geraOcorrencia() {
        return this == PRECISA_AJUDA || this == SEM_CONTATO;
    }
}