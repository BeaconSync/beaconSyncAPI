package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.auditoria.AuditoriaService;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.distrito.Distrito;
import br.com.fiap.beaconsync.distrito.DistritoRepository;
import br.com.fiap.beaconsync.exception.RegraNegocioException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final PessoaVulneravelRepository pessoaRepository;
    private final EnderecoRepository enderecoRepository;
    private final DistritoRepository distritoRepository;
    private final AuthorizationService authorizationService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<EnderecoResponseDTO> listar(Long pessoaId) {
        authorizationService.exigirVinculoAtivo(pessoaId);
        PessoaVulneravel pessoa = buscarPessoa(pessoaId);
        Long atualId = pessoa.getEnderecoAtual() != null ? pessoa.getEnderecoAtual().getId() : null;
        return pessoa.getEnderecos().stream()
                .map(endereco -> new EnderecoResponseDTO(endereco, atualId))
                .toList();
    }

    @Transactional
    public EnderecoResponseDTO adicionar(Long pessoaId, EnderecoRequestDTO dto) {
        authorizationService.exigirVinculoAtivo(pessoaId);
        PessoaVulneravel pessoa = buscarPessoa(pessoaId);

        Endereco endereco = montar(dto);
        pessoa.adicionarEndereco(endereco);
        enderecoRepository.save(endereco);

        auditoriaService.registrar(authorizationService.usuarioLogadoId(), "CRIAR", "ENDERECO",
                endereco.getId(), "Endereço '" + endereco.getApelido() + "' da pessoa " + pessoaId);
        Long atualId = pessoa.getEnderecoAtual() != null ? pessoa.getEnderecoAtual().getId() : null;
        return new EnderecoResponseDTO(endereco, atualId);
    }

    @Transactional
    public PessoaResponseDTO definirAtual(Long pessoaId, EnderecoAtualRequestDTO dto) {
        authorizationService.exigirVinculoAtivo(pessoaId);
        PessoaVulneravel pessoa = buscarPessoa(pessoaId);
        Endereco endereco = buscarEnderecoDaPessoa(pessoaId, dto.enderecoId());

        pessoa.setEnderecoAtual(endereco);

        auditoriaService.registrar(authorizationService.usuarioLogadoId(), "ALTERAR_ENDERECO_ATUAL",
                "PESSOA_VULNERAVEL", pessoaId, "Endereço atual: " + endereco.getId());
        return new PessoaResponseDTO(pessoa);
    }

    @Transactional
    public void remover(Long pessoaId, Long enderecoId) {
        authorizationService.exigirVinculoAtivo(pessoaId);
        PessoaVulneravel pessoa = buscarPessoa(pessoaId);
        Endereco endereco = buscarEnderecoDaPessoa(pessoaId, enderecoId);

        if (pessoa.getEnderecoAtual() != null && pessoa.getEnderecoAtual().getId().equals(enderecoId)) {
            throw new RegraNegocioException("Este é o endereço atual. Defina outro endereço atual antes de removê-lo.");
        }
        pessoa.getEnderecos().remove(endereco);

        auditoriaService.registrar(authorizationService.usuarioLogadoId(), "EXCLUIR", "ENDERECO",
                enderecoId, "Endereço da pessoa " + pessoaId);
    }

    public Endereco montar(EnderecoRequestDTO dto) {
        Distrito distrito = distritoRepository.findById(dto.distritoId())
                .orElseThrow(() -> new RegraNegocioException("Distrito " + dto.distritoId() + " não existe."));

        Endereco endereco = new Endereco();
        endereco.setApelido(dto.apelido().trim());
        endereco.setLogradouro(dto.logradouro().trim());
        endereco.setNumero(dto.numero().trim());
        endereco.setComplemento(dto.complemento());
        endereco.setCep(dto.cep().replace("-", ""));
        endereco.setDistrito(distrito);
        return endereco;
    }

    private PessoaVulneravel buscarPessoa(Long pessoaId) {
        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));
    }

    private Endereco buscarEnderecoDaPessoa(Long pessoaId, Long enderecoId) {
        return enderecoRepository.findById(enderecoId)
                .filter(endereco -> endereco.getPessoa().getId().equals(pessoaId))
                .orElseThrow(() -> new RegraNegocioException("O endereço informado não pertence a esta pessoa."));
    }
}