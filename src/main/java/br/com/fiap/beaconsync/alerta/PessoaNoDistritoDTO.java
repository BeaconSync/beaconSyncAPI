package br.com.fiap.beaconsync.alerta;

import br.com.fiap.beaconsync.pessoa.TipoVulnerabilidade;
import br.com.fiap.beaconsync.resgate.StatusOcorrencia;

import java.util.List;

/**
 * Linha da lista que o agente vê ao clicar num distrito do mapa.
 * Só existe durante um alerta ativo (regra de privacidade).
 */
public record PessoaNoDistritoDTO(
        Long situacaoId,
        Long pessoaId,
        String nome,
        int idade,
        TipoVulnerabilidade tipoVulnerabilidade,
        String necessidadesRemocao,
        String enderecoApelido,
        String logradouro,
        String numero,
        String complemento,
        String cep,
        Situacao situacao,
        Integer gravidade,
        Boolean riscoImediato,
        String resumoEquipe,
        long minutosEmAberto,
        Long ocorrenciaId,
        StatusOcorrencia ocorrenciaStatus,
        List<ContatoResponsavelDTO> responsaveis
) {
    public record ContatoResponsavelDTO(String nome, String telefone, String parentesco) {}
}