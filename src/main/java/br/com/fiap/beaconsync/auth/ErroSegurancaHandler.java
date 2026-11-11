package br.com.fiap.beaconsync.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class ErroSegurancaHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException e) throws IOException {
        escrever(response, HttpServletResponse.SC_UNAUTHORIZED, "Não Autenticado",
                "Token ausente, inválido ou expirado. Faça login e envie Authorization: Bearer <token>.",
                request.getRequestURI());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException e) throws IOException {
        escrever(response, HttpServletResponse.SC_FORBIDDEN, "Acesso Negado",
                "Seu perfil não tem permissão para este recurso.", request.getRequestURI());
    }

    private void escrever(HttpServletResponse response, int status, String erro,
                          String mensagem, String path) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = """
                {"timestamp":"%s","status":%d,"erro":"%s","mensagem":"%s","path":"%s"}"""
                .formatted(LocalDateTime.now(), status, erro, mensagem, escapar(path));
        response.getWriter().write(json);
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}