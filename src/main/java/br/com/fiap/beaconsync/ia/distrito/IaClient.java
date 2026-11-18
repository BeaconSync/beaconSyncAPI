package br.com.fiap.beaconsync.ia.distrito;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

@Component
public class IaClient {
    private final RestClient restClient;
    private final IaProperties properties;

    public IaClient(@Qualifier("beaconIaRestClient") RestClient restClient, IaProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public IaAnaliseResponse analisar(IaAnaliseRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IaIndisponivelException(503, "O serviço de análise ainda não foi configurado no Java.");
        }
        try {
            IaAnaliseResponse response = restClient.post().uri("/ia/analisar-distrito")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", properties.getApiKey())
                    .body(request).retrieve().body(IaAnaliseResponse.class);
            validar(request, response);
            return response;
        } catch (RestClientResponseException e) {
            int externo = e.getStatusCode().value();
            if (externo == 504) throw new IaIndisponivelException(504, "A análise demorou para responder. O mapa continua disponível.");
            if (externo == 429 || externo == 503) throw new IaIndisponivelException(503, "Serviço de análise temporariamente indisponível.");
            throw new IaIndisponivelException(502, "Falha na comunicação com o serviço de análise.");
        } catch (ResourceAccessException e) {
            for (Throwable causa = e; causa != null; causa = causa.getCause()) {
                if (causa instanceof HttpTimeoutException || causa instanceof SocketTimeoutException) {
                    throw new IaIndisponivelException(504, "A análise demorou para responder. O mapa continua disponível.");
                }
            }
            throw new IaIndisponivelException(503, "Não foi possível conectar ao serviço de análise.");
        } catch (RestClientException e) {
            throw new IaIndisponivelException(502, "O serviço de análise retornou uma resposta inválida.");
        }
    }

    private void validar(IaAnaliseRequest request, IaAnaliseResponse response) {
        long acompanhamento = request.situacoes().precisaAjuda() + request.situacoes().semContato()
                + request.situacoes().pendenteConfirmacao();
        long total = acompanhamento + request.situacoes().segura() + request.situacoes().resgatada();
        boolean valido = response != null && response.idAnalise() != null && !response.idAnalise().isBlank()
                && Objects.equals(request.alertaId(), response.alertaId())
                && Objects.equals(request.distritoId(), response.distritoId())
                && Objects.equals(acompanhamento, response.totalAcompanhamento())
                && Objects.equals(total, response.totalCadastrado())
                && response.status() != null && Set.of("GERADO", "DEMONSTRACAO", "SEM_BASE").contains(response.status())
                && response.modo() != null && Set.of("gemini", "demonstracao").contains(response.modo())
                && response.analise() != null && !response.analise().isBlank()
                && response.boletimDados() != null && !response.boletimDados().isBlank()
                && response.fontesRecuperadas() != null && response.avisos() != null;
        if (valido) {
            try {
                valido = OffsetDateTime.parse(request.atualizadoEm()).toInstant()
                        .equals(OffsetDateTime.parse(response.dadosAtualizadosEm()).toInstant());
                OffsetDateTime.parse(response.geradoEm());
            } catch (RuntimeException e) { valido = false; }
        }
        if (valido && "GERADO".equals(response.status())) {
            valido = "gemini".equals(response.modo()) && response.modelo() != null && !response.modelo().isBlank();
        }
        if (valido && "DEMONSTRACAO".equals(response.status())) {
            valido = "demonstracao".equals(response.modo());
        }
        if (!valido) throw new IaIndisponivelException(502, "Resposta de análise incompatível com o distrito consultado.");
    }
}