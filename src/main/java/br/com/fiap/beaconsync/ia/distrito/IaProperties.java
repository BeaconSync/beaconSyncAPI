package br.com.fiap.beaconsync.ia.distrito;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties("beaconsync.ia.distrito")
public class IaProperties {
    @NotNull
    private URI baseUrl = URI.create("https://beaconsync-ia.onrender.com");
    private String apiKey = "";
    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(10);
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(90);

    @AssertTrue(message = "Use HTTPS para a IA ou HTTP somente em localhost/127.0.0.1.")
    public boolean isUrlSegura() {
        if (baseUrl == null || baseUrl.getHost() == null || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null || baseUrl.getFragment() != null) return false;
        return "https".equals(baseUrl.getScheme()) || ("http".equals(baseUrl.getScheme())
                && ("localhost".equals(baseUrl.getHost()) || "127.0.0.1".equals(baseUrl.getHost())));
    }

    @AssertTrue(message = "Timeouts da IA devem ser positivos e de no máximo 180 segundos.")
    public boolean isTimeoutsValidos() {
        return valido(connectTimeout) && valido(readTimeout);
    }

    private boolean valido(Duration value) {
        return value != null && value.compareTo(Duration.ofMillis(1)) >= 0
                && value.compareTo(Duration.ofSeconds(180)) <= 0;
    }
}