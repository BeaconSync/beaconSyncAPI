package br.com.fiap.beaconsync.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroValidacao> tratarValidacao(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ErroValidacao.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroValidacao.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();

        ErroValidacao erro = new ErroValidacao(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de Validação de Dados",
                request.getRequestURI(),
                campos
        );
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroPadrao> tratarJsonInvalido(HttpMessageNotReadableException e, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição Inválida",
                "O corpo da requisição está mal formado ou tem um valor inválido (confira datas e enums).", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroPadrao> tratarParametroInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro Inválido",
                "O valor informado em '" + e.getName() + "' não é válido.", request);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroPadrao> tratarOrdenacaoInvalida(PropertyReferenceException e, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro de Ordenação Inválido",
                "O campo '" + e.getPropertyName() + "' informado em 'sort' não existe neste recurso.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroPadrao> tratar401(AuthenticationException e, HttpServletRequest request) {
        return resposta(HttpStatus.UNAUTHORIZED, "Não Autenticado", e.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroPadrao> tratar403(AccessDeniedException e, HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso Negado", e.getMessage(), request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErroPadrao> tratar404(EntityNotFoundException e, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "Não Encontrado", e.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroPadrao> tratarRotaInexistente(NoResourceFoundException e, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "Não Encontrado", "Rota não encontrada.", request);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroConflito> tratarConflito(ConflitoException e, HttpServletRequest request) {
        ErroConflito erro = new ErroConflito(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflito",
                e.getMessage(),
                request.getRequestURI(),
                e.getRecursoId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroPadrao> tratarIntegridade(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Violação de integridade em {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "Conflito",
                "A operação conflita com dados já existentes ou ainda em uso.", request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroPadrao> tratarRegraNegocio(RegraNegocioException e, HttpServletRequest request) {
        // 422: o nome da constante mudou entre versões do Spring, então usamos o código
        return resposta(HttpStatus.valueOf(422), "Regra de Negócio", e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroPadrao> tratar500(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno no Servidor",
                "Ocorreu um erro inesperado. Tente novamente.", request);
    }

    private ResponseEntity<ErroPadrao> resposta(HttpStatus status, String titulo, String mensagem,
                                                HttpServletRequest request) {
        ErroPadrao erro = new ErroPadrao(
                LocalDateTime.now(),
                status.value(),
                titulo,
                mensagem,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(erro);
    }
}