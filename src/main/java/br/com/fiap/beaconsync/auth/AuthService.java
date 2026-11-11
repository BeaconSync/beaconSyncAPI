package br.com.fiap.beaconsync.auth;

import br.com.fiap.beaconsync.exception.ConflitoException;
import br.com.fiap.beaconsync.exception.RegraNegocioException;
import br.com.fiap.beaconsync.resgate.Resgatista;
import br.com.fiap.beaconsync.resgate.ResgatistaRepository;
import br.com.fiap.beaconsync.resgate.TipoResgatista;
import br.com.fiap.beaconsync.usuario.Papel;
import br.com.fiap.beaconsync.usuario.Usuario;
import br.com.fiap.beaconsync.usuario.UsuarioRepository;
import br.com.fiap.beaconsync.usuario.UsuarioResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ResgatistaRepository resgatistaRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(dto.email().trim())
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .filter(u -> passwordEncoder.matches(dto.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));

        return new LoginResponseDTO(
                tokenService.gerarToken(usuario),
                "Bearer",
                tokenService.getExpiracaoSegundos(),
                new UsuarioResponseDTO(usuario)
        );
    }

    @Transactional
    public UsuarioResponseDTO registrar(RegistroRequestDTO dto) {
        if (dto.papel() != Papel.RESPONSAVEL && dto.papel() != Papel.VOLUNTARIO) {
            throw new RegraNegocioException("No autocadastro só é possível escolher RESPONSAVEL ou VOLUNTARIO. "
                    + "Profissionais e agentes são cadastrados pelo órgão.");
        }
        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("E-mail já cadastrado.", null);
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(dto.senha()));
        usuario.setTelefone(dto.telefone());
        usuario.setPapel(dto.papel());
        usuarioRepository.save(usuario);

        if (dto.papel() == Papel.VOLUNTARIO) {
            Resgatista resgatista = new Resgatista();
            resgatista.setUsuario(usuario);
            resgatista.setTipo(TipoResgatista.VOLUNTARIO);
            resgatistaRepository.save(resgatista);
        }

        return new UsuarioResponseDTO(usuario);
    }
}