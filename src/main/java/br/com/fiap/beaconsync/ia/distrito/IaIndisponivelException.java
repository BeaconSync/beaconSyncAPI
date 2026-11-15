package br.com.fiap.beaconsync.ia.distrito;

public class IaIndisponivelException extends RuntimeException {
    private final int status;
    public IaIndisponivelException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
    public int getStatus() { return status; }
}