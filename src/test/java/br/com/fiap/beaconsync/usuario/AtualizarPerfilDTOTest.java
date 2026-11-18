package br.com.fiap.beaconsync.usuario;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AtualizarPerfilDTOTest {
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    @AfterAll static void fechar() { factory.close(); }

    @Test void emailOmitidoContinuaCompativel() {
        assertTrue(factory.getValidator().validate(
                new AtualizarPerfilDTO("Ana", "11999999999")).isEmpty());
    }

    @Test void rejeitaEmailVazioInvalidoOuLongo() {
        for (String email : new String[]{"", "   ", "sem-arroba", "a b@example.com", "a".repeat(150) + "@example.com"}) {
            var erros = factory.getValidator().validate(new AtualizarPerfilDTO("Ana", "11999999999", null, email));
            assertTrue(erros.stream().anyMatch(e -> e.getPropertyPath().toString().equals("email")), email);
        }
    }

    @Test void normalizaAntesDaValidacao() {
        var dto = new AtualizarPerfilDTO("Ana", "11999999999", null, "  ANA@EXAMPLE.COM  ");
        assertEquals("ana@example.com", dto.email());
        assertTrue(factory.getValidator().validate(dto).isEmpty());
    }
}
