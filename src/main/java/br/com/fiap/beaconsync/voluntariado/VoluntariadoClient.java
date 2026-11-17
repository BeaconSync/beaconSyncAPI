package br.com.fiap.beaconsync.voluntariado;

import br.com.fiap.beaconsync.exception.RegraNegocioException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

@Component
public class VoluntariadoClient {
    private static final String ROTA = "/api/v1/voluntarios/me";
    private final RestClient client;
    private final NetTokenService tokens;
    private final String issuer;

    public VoluntariadoClient(
            @Qualifier("voluntariadoRestClient") RestClient client,
            NetTokenService tokens,
            @Value("${beaconsync.jwt.issuer}") String issuer) {
        this.client = client;
        this.tokens = tokens;
        this.issuer = issuer;
    }

    public boolean disponivel(Long usuarioId) {
        return consultar(usuarioId).map(Perfil::disponivelParaAjuda).orElse(false);
    }

    public Optional<Perfil> consultar(Long usuarioId) {
        try {
            var perfil =
                    client.get()
                            .uri(ROTA)
                            .headers(h -> h.setBearerAuth(tokens.emitir(usuarioId)))
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(Perfil.class);
            return Optional.of(validar(usuarioId, perfil));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new NetIndisponivelException();
        }
    }

    public Perfil alterarDisponibilidade(Long usuarioId, boolean disponivel) {
        try {
            var perfil =
                    client.patch()
                            .uri(ROTA + "/disponibilidade")
                            .headers(h -> h.setBearerAuth(tokens.emitir(usuarioId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Map.of("disponivelParaAjuda", disponivel))
                            .retrieve()
                            .body(Perfil.class);
            return validar(usuarioId, perfil);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RegraNegocioException("Cadastre primeiro seu voluntariado na API .NET.");
        } catch (RestClientException e) {
            throw new NetIndisponivelException();
        }
    }

    private Perfil validar(Long usuarioId, Perfil perfil) {
        if (perfil == null
                || !usuarioId.toString().equals(perfil.usuarioId())
                || !issuer.equals(perfil.usuarioEmissor())
                || perfil.disponivelParaAjuda() == null) throw new NetIndisponivelException();
        return perfil;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Perfil(String usuarioId, String usuarioEmissor, Boolean disponivelParaAjuda) {}
}