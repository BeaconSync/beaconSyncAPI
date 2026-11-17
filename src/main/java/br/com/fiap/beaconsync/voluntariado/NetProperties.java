package br.com.fiap.beaconsync.voluntariado;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties("beaconsync.net")
public class NetProperties {

    /** NET_INTEGRACAO: true consulta o .NET; false usa a disponibilidade guardada no Java. */
    private boolean integracao = false;

    private URI baseUrl = URI.create("https://beaconsync-rastreador-de.onrender.com");
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);

    @AssertTrue(message = "Timeouts do .NET devem estar entre 1 ms e 30 segundos.")
    public boolean isTimeoutsValidos() {
        return valido(connectTimeout) && valido(readTimeout);
    }

    private boolean valido(Duration d) {
        return d != null
                && d.compareTo(Duration.ofMillis(1)) >= 0
                && d.compareTo(Duration.ofSeconds(30)) <= 0;
    }
}