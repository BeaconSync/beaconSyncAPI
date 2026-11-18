package br.com.fiap.beaconsync.ia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IaDisponibilidade {

    private static final String SEM_CHAVE = "sem-chave";

    private final boolean habilitada;

    public IaDisponibilidade(@Value("${beaconsync.ia.provedor}") String provedor,
                             @Value("${spring.ai.openai.api-key}") String chave) {
        this.habilitada = "openai".equalsIgnoreCase(provedor.trim())
                && chave != null && !chave.isBlank() && !SEM_CHAVE.equals(chave.trim());
        log.info("IA generativa {}", habilitada
                ? "LIGADA (OpenAI via Spring AI)"
                : "DESLIGADA (triagem por regra e boletim por modelo de texto)");
    }

    public boolean habilitada() {
        return habilitada;
    }
}