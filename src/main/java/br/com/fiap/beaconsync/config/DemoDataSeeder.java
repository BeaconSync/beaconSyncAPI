package br.com.fiap.beaconsync.config;

import br.com.fiap.beaconsync.distrito.Distrito;
import br.com.fiap.beaconsync.distrito.DistritoRepository;
import br.com.fiap.beaconsync.pessoa.Endereco;
import br.com.fiap.beaconsync.pessoa.PessoaVulneravel;
import br.com.fiap.beaconsync.pessoa.PessoaVulneravelRepository;
import br.com.fiap.beaconsync.pessoa.StatusVinculo;
import br.com.fiap.beaconsync.pessoa.TipoVulnerabilidade;
import br.com.fiap.beaconsync.pessoa.VinculoResponsavel;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dados de demonstração para o mapa ter todas as cores no vídeo e na apresentação.
 * Cria pessoas (CPFs fictícios, mas válidos) ligadas ao responsável demo@beaconsync.com:
 *   Lapa 32 (vermelho), Barra Funda 12 (laranja), Vila Leopoldina 5 (amarelo).
 * Emitindo o alerta para esses 3 distritos + Jaguaré (0, azul), aparecem as 4 cores.
 * Desligue com SEED_DEMO=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private static final String EMAIL_DEMO = "demo@beaconsync.com";

    private static final String[] NOMES = {
            "Maria", "José", "Antônio", "Francisca", "Ana", "João", "Luiza", "Pedro",
            "Helena", "Sebastião", "Benedita", "Raimundo", "Teresa", "Joaquim", "Rosa", "Manoel"
    };
    private static final String[] SOBRENOMES = {
            "Silva", "Santos", "Oliveira", "Souza", "Lima", "Pereira", "Costa", "Ferreira", "Almeida", "Ribeiro"
    };
    private static final TipoVulnerabilidade[] TIPOS = TipoVulnerabilidade.values();

    private final UsuarioRepository usuarioRepository;
    private final DistritoRepository distritoRepository;
    private final PessoaVulneravelRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${beaconsync.seed.demo}")
    private boolean habilitado;

    @Value("${beaconsync.seed.senha}")
    private String senha;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitado || senha == null || senha.isBlank() || usuarioRepository.existsByEmailIgnoreCase(EMAIL_DEMO)) {
            return;
        }

        Usuario responsavelDemo = new Usuario();
        responsavelDemo.setNome("Responsável Demonstração");
        responsavelDemo.setEmail(EMAIL_DEMO);
        responsavelDemo.setTelefone("11990000099");
        responsavelDemo.setPapel(Papel.RESPONSAVEL);
        responsavelDemo.setSenhaHash(passwordEncoder.encode(senha));
        usuarioRepository.save(responsavelDemo);

        int indice = 0;
        indice = criarNoDistrito("Lapa", 32, indice, responsavelDemo);
        indice = criarNoDistrito("Barra Funda", 12, indice, responsavelDemo);
        indice = criarNoDistrito("Vila Leopoldina", 5, indice, responsavelDemo);

        log.info("Dados de demonstração criados: {} pessoas ligadas a {}", indice, EMAIL_DEMO);
    }

    private int criarNoDistrito(String nomeDistrito, int quantidade, int indiceInicial, Usuario responsavel) {
        Distrito distrito = distritoRepository.findByNomeIgnoreCase(nomeDistrito).orElseThrow();
        int indice = indiceInicial;
        for (int i = 0; i < quantidade; i++, indice++) {
            TipoVulnerabilidade tipo = TIPOS[indice % TIPOS.length];

            PessoaVulneravel pessoa = new PessoaVulneravel();
            pessoa.setNome(NOMES[indice % NOMES.length] + " " + SOBRENOMES[(indice * 7) % SOBRENOMES.length]
                    + " " + SOBRENOMES[(indice * 3 + 1) % SOBRENOMES.length]);
            pessoa.setCpf(cpfValido(100_000_001L + indice * 9_973L));
            pessoa.setTipoVulnerabilidade(tipo);
            pessoa.setDataNascimento(tipo == TipoVulnerabilidade.BEBE
                    ? LocalDate.now().minusMonths(3 + indice % 18)
                    : LocalDate.of(1935 + indice % 40, 1 + indice % 12, 1 + indice % 28));
            pessoa.setNecessidadesRemocao(necessidades(tipo));
            pessoa.setConsentimentoEm(LocalDateTime.now());

            Endereco endereco = new Endereco();
            endereco.setApelido("Casa");
            endereco.setLogradouro("Rua Demonstração " + nomeDistrito);
            endereco.setNumero(String.valueOf(10 + indice));
            endereco.setCep("05" + String.format("%06d", 1000 + indice));
            endereco.setDistrito(distrito);
            pessoa.adicionarEndereco(endereco);

            VinculoResponsavel vinculo = new VinculoResponsavel();
            vinculo.setPessoa(pessoa);
            vinculo.setUsuario(responsavel);
            vinculo.setParentesco("familiar");
            vinculo.setStatus(StatusVinculo.ATIVO);
            pessoa.getVinculos().add(vinculo);

            pessoaRepository.saveAndFlush(pessoa);
            pessoa.setEnderecoAtual(endereco);
        }
        return indice;
    }

    private String necessidades(TipoVulnerabilidade tipo) {
        return switch (tipo) {
            case ACAMADO -> "maca";
            case PESSOA_COM_DEFICIENCIA -> "cadeira de rodas";
            case BEBE -> "bebê conforto";
            case DEPENDENTE_APARELHO_MEDICO -> "maca, cilindro de oxigênio";
            case OUTRO -> null;
        };
    }

    /** Monta um CPF válido (com dígitos verificadores) a partir de 9 dígitos. */
    private String cpfValido(long base) {
        String digitos = String.format("%09d", base % 1_000_000_000L);
        int d1 = digitoVerificador(digitos, 10);
        int d2 = digitoVerificador(digitos + d1, 11);
        return digitos + d1 + d2;
    }

    private int digitoVerificador(String digitos, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < digitos.length(); i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * (pesoInicial - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}