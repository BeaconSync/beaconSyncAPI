package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.voluntariado.NetIndisponivelException;
import br.com.fiap.beaconsync.voluntariado.NetProperties;
import br.com.fiap.beaconsync.voluntariado.VoluntariadoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DisponibilidadeService {

    private final VoluntariadoClient voluntariado;
    private final boolean integracao;

    public DisponibilidadeService(VoluntariadoClient voluntariado, NetProperties properties) {
        this.voluntariado = voluntariado;
        this.integracao = properties.isIntegracao();
        log.info("Integração com o .NET {}", integracao
                ? "LIGADA (disponibilidade dos voluntários vem da API .NET)"
                : "DESLIGADA (disponibilidade dos voluntários fica no Java)");
    }

    public boolean disponivel(Resgatista resgatista) {
        if (usaNet(resgatista)) {
            return voluntariado.disponivel(resgatista.getUsuario().getId());
        }
        return Boolean.TRUE.equals(resgatista.getDisponivel());
    }

    public boolean disponivelParaListagem(Resgatista resgatista) {
        try {
            return disponivel(resgatista);
        } catch (NetIndisponivelException e) {
            log.warn("Sem resposta do .NET para o resgatista {}; ele fica fora da lista.", resgatista.getId());
            return false;
        }
    }

    public boolean alterar(Resgatista resgatista, boolean disponivel) {
        if (usaNet(resgatista)) {
            return voluntariado
                    .alterarDisponibilidade(resgatista.getUsuario().getId(), disponivel)
                    .disponivelParaAjuda();
        }
        resgatista.setDisponivel(disponivel);
        return disponivel;
    }

    private boolean usaNet(Resgatista resgatista) {
        return integracao && resgatista.getTipo() == TipoResgatista.VOLUNTARIO;
    }
}