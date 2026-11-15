package br.com.fiap.beaconsync.ia.distrito;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IaProperties.class)
public class IaConfig {
    @Bean
    public Clock iaDistritoClock() { return Clock.systemUTC(); }

    @Bean
    public RestClient beaconIaRestClient(IaProperties properties) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().requestFactory(factory)
                .baseUrl(properties.getBaseUrl().toString()).build();
    }
}