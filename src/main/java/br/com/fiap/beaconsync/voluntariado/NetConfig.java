package br.com.fiap.beaconsync.voluntariado;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NetProperties.class)
public class NetConfig {
    @Bean
    public RestClient voluntariadoRestClient(NetProperties p) {
        var uri = p.getBaseUrl();
        boolean local =
                uri != null
                        && uri.getHost() != null
                        && Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost());
        if (uri == null
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))
                || !("https".equals(uri.getScheme()) || local && "http".equals(uri.getScheme())))
            throw new IllegalArgumentException(
                    "NET_BASE_URL deve ser origem HTTPS; HTTP só em localhost.");
        var http =
                HttpClient.newBuilder()
                        .connectTimeout(p.getConnectTimeout())
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(p.getReadTimeout());
        return RestClient.builder().requestFactory(factory).baseUrl(uri.toString()).build();
    }
}