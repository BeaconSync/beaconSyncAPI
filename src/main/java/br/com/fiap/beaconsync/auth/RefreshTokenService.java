package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.usuario.Usuario;
import br.com.fiap.beaconsync.usuario.UsuarioResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final SessaoRefreshRepository sessoes;
    private final RefreshTokenRepository tokens;
    private final TokenService jwt;
    private final long validadeSegundos;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock = Clock.systemUTC();

    public RefreshTokenService(SessaoRefreshRepository sessoes, RefreshTokenRepository tokens,
                               TokenService jwt,
                               @Value("${beaconsync.jwt.refresh-expiracao-segundos:604800}") long validadeSegundos) {
        if (validadeSegundos < 3600 || validadeSegundos > 2_592_000) {
            throw new IllegalArgumentException("A sessão de refresh deve durar de 1 hora a 30 dias.");
        }
        this.sessoes = sessoes;
        this.tokens = tokens;
        this.jwt = jwt;
        this.validadeSegundos = validadeSegundos;
    }

    @Transactional
    public LoginResponseDTO criarSessao(Usuario usuario) {
        var agora = agora();
        var sessao = new SessaoRefresh();
        sessao.setId(UUID.randomUUID().toString());
        sessao.setUsuario(usuario);
        sessao.setCriadaEm(agora);
        sessao.setExpiraEm(agora.plusSeconds(validadeSegundos));
        sessao = sessoes.save(sessao);
        return emitir(sessao, agora);
    }

    @Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
    public LoginResponseDTO renovar(String segredo) {
        var hash = hashValido(segredo);
        var sessao = buscarSessao(hash);
        var agora = agora();
        if (sessao.getRevogadaEm() != null) throw new RefreshTokenInvalidoException();
        if (!sessao.getExpiraEm().isAfter(agora)
                || !Boolean.TRUE.equals(sessao.getUsuario().getAtivo())) {
            sessao.setRevogadaEm(agora);
            throw new RefreshTokenInvalidoException();
        }
        var token = tokens.findById(hash).orElseThrow(RefreshTokenInvalidoException::new);
        if (token.getUsadoEm() != null) {
            sessao.setRevogadaEm(agora);
            throw new RefreshTokenInvalidoException();
        }
        token.setUsadoEm(agora);
        return emitir(sessao, agora);
    }

    @Transactional
    public void encerrar(String segredo) {
        var hash = hashValido(segredo);
        tokens.buscarSessaoId(hash).flatMap(sessoes::buscarComBloqueio).ifPresent(sessao -> {
            if (sessao.getRevogadaEm() == null) sessao.setRevogadaEm(agora());
        });
    }

    private SessaoRefresh buscarSessao(String hash) {
        return tokens.buscarSessaoId(hash).flatMap(sessoes::buscarComBloqueio)
                .orElseThrow(RefreshTokenInvalidoException::new);
    }

    private LoginResponseDTO emitir(SessaoRefresh sessao, LocalDateTime agora) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String segredo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var token = new RefreshToken();
        token.setTokenHash(hashValido(segredo));
        token.setSessao(sessao);
        token.setCriadoEm(agora);
        tokens.save(token);
        var usuario = sessao.getUsuario();
        return new LoginResponseDTO(jwt.gerarToken(usuario), "Bearer", jwt.getExpiracaoSegundos(),
                new UsuarioResponseDTO(usuario), segredo,
                Math.max(0, Duration.between(agora, sessao.getExpiraEm()).getSeconds()));
    }

    static String hashValido(String segredo) {
        if (segredo == null || !segredo.matches("[A-Za-z0-9_-]{43}")) {
            throw new RefreshTokenInvalidoException();
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(segredo.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }

    private LocalDateTime agora() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
}