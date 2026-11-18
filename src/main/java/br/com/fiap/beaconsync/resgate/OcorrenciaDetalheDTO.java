package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.alerta.OrigemTriagem;
import br.com.fiap.beaconsync.alerta.Situacao;
import br.com.fiap.beaconsync.pessoa.TipoVulnerabilidade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OcorrenciaDetalheDTO(
        Long id,
        StatusOcorrencia status,
        BigDecimal scorePrioridade,
        Integer prioridadeManual,
        String orgaoNome,
        Long alertaId,
        String alertaTitulo,
        Long pessoaId,
        String pessoaNome,
        int idade,
        TipoVulnerabilidade tipoVulnerabilidade,
        String necessidadesRemocao,
        Situacao situacao,
        String relatoResponsavel,
        Integer gravidade,
        Boolean riscoImediato,
        String necessidadesTriagem,
        String resumoEquipe,
        OrigemTriagem origemTriagem,
        EnderecoResgateDTO endereco,
        List<ContatoDTO> responsaveis,
        List<MembroEquipeDTO> equipe,
        List<HistoricoDTO> historico,
        LocalDateTime criadaEm,
        LocalDateTime concluidaEm
) {
    public record EnderecoResgateDTO(String apelido, String logradouro, String numero, String complemento,
                                     String cep, String distrito) {}

    public record ContatoDTO(String nome, String telefone, String parentesco) {}

    public record MembroEquipeDTO(Long participacaoId, Long resgatistaId, String nome, String telefone,
                                  TipoResgatista tipo, String funcao, LocalDateTime entrouEm) {}

    public record HistoricoDTO(StatusOcorrencia statusAnterior, StatusOcorrencia statusNovo, String alteradoPor,
                               LocalDateTime alteradoEm, String observacao) {}
}