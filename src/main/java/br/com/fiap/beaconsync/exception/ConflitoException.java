package br.com.fiap.beaconsync.exception;

import lombok.Getter;

@Getter
public class ConflitoException extends RuntimeException {

    private final Long recursoId;

    public ConflitoException(String mensagem, Long recursoId) {
        super(mensagem);
        this.recursoId = recursoId;
    }
}