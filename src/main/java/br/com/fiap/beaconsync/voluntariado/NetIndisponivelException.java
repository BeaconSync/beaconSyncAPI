package br.com.fiap.beaconsync.voluntariado;

public class NetIndisponivelException extends RuntimeException {
    public NetIndisponivelException() {
        super("Não foi possível confirmar a disponibilidade no .NET. Tente novamente mais tarde.");
    }
}