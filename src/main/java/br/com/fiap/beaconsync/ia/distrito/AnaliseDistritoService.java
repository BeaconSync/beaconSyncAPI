package br.com.fiap.beaconsync.ia.distrito;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnaliseDistritoService {
    private final DadosDistritoService dados;
    private final IaClient client;

    public IaAnaliseResponse analisar(Long alertaId, Long distritoId, String pergunta) {
        var request = dados.consultar(alertaId, distritoId, pergunta);
        return client.analisar(request);
    }
}