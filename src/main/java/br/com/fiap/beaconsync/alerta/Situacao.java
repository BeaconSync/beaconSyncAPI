package br.com.fiap.beaconsync.alerta;

public enum Situacao {
    PENDENTE,
    SEGURA,
    PRECISA_AJUDA,
    SEM_CONTATO,
    RESGATADA;

    public boolean contaNoMapa() {
        return this == PENDENTE || this == PRECISA_AJUDA || this == SEM_CONTATO;
    }

    public boolean geraOcorrencia() {
        return this == PRECISA_AJUDA || this == SEM_CONTATO;
    }
}