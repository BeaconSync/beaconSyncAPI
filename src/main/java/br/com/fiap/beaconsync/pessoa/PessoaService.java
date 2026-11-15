package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.alerta.AlertaService;
import br.com.fiap.beaconsync.auditoria.AuditoriaService;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.exception.ConflitoException;
import br.com.fiap.beaconsync.usuario.Usuario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PessoaService {

    private final PessoaVulneravelRepository pessoaRepository;
    private final EnderecoService enderecoService;
    private final AuthorizationService authorizationService;
    private final AuditoriaService auditoriaService;
    private final AlertaService alertaService;

    /**
     * Registro único: se o CPF já existe, não cria outro registro. Devolve 409
     * com o id da pessoa para o app oferecer o pedido de vínculo.
     */
    @Transactional
    public PessoaResponseDTO cadastrar(PessoaRequestDTO dto) {
        String cpf = somenteDigitos(dto.cpf());
        pessoaRepository.findByCpf(cpf).ifPresent(existente -> {
            throw new ConflitoException("Esta pessoa já está cadastrada. Peça para ser vinculado como responsável em "
                    + "POST /api/v1/pessoas/" + existente.getId() + "/vinculos.", existente.getId());
        });

        Usuario responsavel = authorizationService.usuarioLogado();

        PessoaVulneravel pessoa = new PessoaVulneravel();
        pessoa.setNome(dto.nome().trim());
        pessoa.setDataNascimento(dto.dataNascimento());
        pessoa.setCpf(cpf);
        pessoa.setTipoVulnerabilidade(dto.tipoVulnerabilidade());
        pessoa.setNecessidadesRemocao(dto.necessidadesRemocao());
        pessoa.setConsentimentoEm(LocalDateTime.now());

        Endereco endereco = enderecoService.montar(dto.endereco());
        pessoa.adicionarEndereco(endereco);

        VinculoResponsavel vinculo = new VinculoResponsavel();
        vinculo.setPessoa(pessoa);
        vinculo.setUsuario(responsavel);
        vinculo.setParentesco(dto.parentesco().trim());
        vinculo.setStatus(StatusVinculo.ATIVO);
        pessoa.getVinculos().add(vinculo);

        // Grava primeiro (gera os ids) e só depois aponta o endereço atual,
        // por causa da referência circular pessoa <-> endereço
        pessoaRepository.saveAndFlush(pessoa);
        pessoa.setEnderecoAtual(endereco);
        // Cadastro feito durante um alerta ativo na área: a pessoa já entra no alerta
        alertaService.sincronizarEnderecoAtual(pessoa);

        auditoriaService.registrar(responsavel.getId(), "CRIAR", "PESSOA_VULNERAVEL", pessoa.getId(),
                "Cadastro com consentimento registrado");
        return new PessoaResponseDTO(pessoa);
    }

    /** Lista só as pessoas em que o usuário logado é responsável ativo. */
    @Transactional(readOnly = true)
    public Page<PessoaResponseDTO> listarMinhas(Pageable pageable) {
        return pessoaRepository
                .findVinculadas(authorizationService.usuarioLogadoId(), StatusVinculo.ATIVO, pageable)
                .map(PessoaResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public PessoaResponseDTO buscarPorId(Long id) {
        // Checa o vínculo antes de buscar: quem não é responsável recebe 403,
        // sem descobrir se o id existe
        authorizationService.exigirVinculoAtivo(id);
        return new PessoaResponseDTO(buscar(id));
    }

    @Transactional
    public PessoaResponseDTO atualizar(Long id, PessoaUpdateDTO dto) {
        authorizationService.exigirVinculoAtivo(id);
        PessoaVulneravel pessoa = buscar(id);

        pessoa.setNome(dto.nome().trim());
        pessoa.setDataNascimento(dto.dataNascimento());
        pessoa.setTipoVulnerabilidade(dto.tipoVulnerabilidade());
        pessoa.setNecessidadesRemocao(dto.necessidadesRemocao());

        auditoriaService.registrar(authorizationService.usuarioLogadoId(), "ALTERAR", "PESSOA_VULNERAVEL", id,
                "Dados cadastrais atualizados");
        return new PessoaResponseDTO(pessoa);
    }

    /** Remove o cadastro (revogação do consentimento). */
    @Transactional
    public void excluir(Long id) {
        authorizationService.exigirVinculoAtivo(id);
        auditoriaService.registrar(authorizationService.usuarioLogadoId(), "EXCLUIR", "PESSOA_VULNERAVEL", id,
                "Cadastro removido (consentimento revogado)");
        pessoaRepository.excluirPorId(id);
    }

    private PessoaVulneravel buscar(Long id) {
        return pessoaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));
    }

    private String somenteDigitos(String texto) {
        return texto.replaceAll("\\D", "");
    }
}