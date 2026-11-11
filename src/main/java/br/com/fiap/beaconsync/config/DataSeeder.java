package br.com.fiap.beaconsync.config;

import br.com.fiap.beaconsync.orgao.Orgao;
import br.com.fiap.beaconsync.orgao.OrgaoRepository;
import br.com.fiap.beaconsync.orgao.TipoOrgao;
import br.com.fiap.beaconsync.resgate.Resgatista;
import br.com.fiap.beaconsync.resgate.ResgatistaRepository;
import br.com.fiap.beaconsync.resgate.TipoResgatista;
import br.com.fiap.beaconsync.usuario.Papel;
import br.com.fiap.beaconsync.usuario.Usuario;
import br.com.fiap.beaconsync.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria os usuários de teste na subida da API, se ainda não existirem.
 * A senha vem de SEED_PASSWORD (.env), então nem ela nem o hash ficam no
 * repositório. Com SEED_PASSWORD vazio, nada é criado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final OrgaoRepository orgaoRepository;
    private final ResgatistaRepository resgatistaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${beaconsync.seed.enabled}")
    private boolean habilitado;

    @Value("${beaconsync.seed.senha}")
    private String senha;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitado) {
            return;
        }
        if (senha == null || senha.isBlank()) {
            log.warn("SEED_PASSWORD vazio: os usuários de teste não foram criados.");
            return;
        }

        Orgao bombeiros = orgaoRepository.findFirstByTipo(TipoOrgao.BOMBEIROS).orElseThrow();
        Orgao defesaCivil = orgaoRepository.findFirstByTipo(TipoOrgao.DEFESA_CIVIL).orElseThrow();

        criar("Ana Souza", "responsavel@beaconsync.com", "11990000001", Papel.RESPONSAVEL, null, null);
        criar("Bruno Lima", "responsavel2@beaconsync.com", "11990000002", Papel.RESPONSAVEL, null, null);
        criar("Carlos Pereira", "voluntario@beaconsync.com", "11990000003", Papel.VOLUNTARIO, null,
                TipoResgatista.VOLUNTARIO);
        criar("Daniela Rocha", "bombeira@beaconsync.com", "11990000004", Papel.PROFISSIONAL, bombeiros,
                TipoResgatista.PROFISSIONAL);
        criar("Eduardo Alves", "agente@beaconsync.com", "11990000005", Papel.AGENTE_ORGAO, defesaCivil, null);
    }

    private void criar(String nome, String email, String telefone, Papel papel, Orgao orgao,
                       TipoResgatista tipoResgatista) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setTelefone(telefone);
        usuario.setPapel(papel);
        usuario.setOrgao(orgao);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuarioRepository.save(usuario);

        if (tipoResgatista != null) {
            Resgatista resgatista = new Resgatista();
            resgatista.setUsuario(usuario);
            resgatista.setTipo(tipoResgatista);
            resgatista.setOrgao(orgao);
            resgatistaRepository.save(resgatista);
        }
        log.info("Usuário de teste criado: {} ({})", email, papel);
    }
}