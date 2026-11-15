package br.com.fiap.beaconsync.alerta;

import br.com.fiap.beaconsync.distrito.DistritoResponseDTO;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record AlertaResponseDTO(
        Long id,
        String titulo,
        String descricao,
        StatusAlerta status,
        Long orgaoId,
        String orgaoNome,
        List<DistritoResponseDTO> distritos,
        long pessoasAfetadas,
        LocalDateTime emitidoEm,
        LocalDateTime encerradoEm
) {
    public AlertaResponseDTO(Alerta alerta, long pessoasAfetadas) {
        this(
                alerta.getId(),
                alerta.getTitulo(),
                alerta.getDescricao(),
                alerta.getStatus(),
                alerta.getOrgao().getId(),
                alerta.getOrgao().getNome(),
                alerta.getDistritos().stream()
                        .map(DistritoResponseDTO::new)
                        .sorted(Comparator.comparing(DistritoResponseDTO::nome))
                        .toList(),
                pessoasAfetadas,
                alerta.getEmitidoEm(),
                alerta.getEncerradoEm()
        );
    }
}