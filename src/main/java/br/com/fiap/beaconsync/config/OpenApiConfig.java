package br.com.fiap.beaconsync.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "BeaconSync API",
        version = "v1",
        description = "Cadastro prévio de pessoas vulneráveis que, no alerta de enchente, "
                + "vira um mapa de quem precisa ser resgatado primeiro e onde a pessoa está."
))
public class OpenApiConfig {
}