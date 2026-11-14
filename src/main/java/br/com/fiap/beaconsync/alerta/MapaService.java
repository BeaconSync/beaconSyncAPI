package br.com.fiap.beaconsync.alerta;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class MapaService {

    /** Situações que ainda contam no mapa (a pessoa não está segura). */
    private static final Set<Situacao> SEM_SEGURANCA =
            EnumSet.of(Situacao.PENDENTE, Situacao.PRECISA_AJUDA, Situacao.SEM_CONTATO);

    private final AlertaRepository alertaRepository;
    private final SituacaoNoAlertaRepository situacaoRepository;

    @Value("${beaconsync.mapa.faixa-amarelo}")
    private long faixaAmarelo;

    @Value("${beaconsync.mapa.faixa-laranja}")
    private long faixaLaranja;

    @Value("${beaconsync.mapa.faixa-vermelho}")
    private long faixaVermelho;

    @Transactional(readOnly = true)
    public MapaResponseDTO gerar(Long alertaId) {
        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new EntityNotFoundException("Alerta não encontrado."));

        // distritoId -> contagem por situação
        Map<Long, Map<Situacao, Long>> contagem = new HashMap<>();
        for (ContagemDistrito linha : situacaoRepository.contarPorDistrito(alertaId, SEM_SEGURANCA)) {
            contagem.computeIfAbsent(linha.getDistritoId(), id -> new HashMap<>())
                    .put(linha.getSituacao(), linha.getTotal());
        }

        // Só os distritos do alerta recebem cor; os de fora não aparecem
        List<MapaDistritoDTO> distritos = alerta.getDistritos().stream()
                .map(distrito -> {
                    Map<Situacao, Long> porSituacao = contagem.getOrDefault(distrito.getId(), Map.of());
                    long pendentes = porSituacao.getOrDefault(Situacao.PENDENTE, 0L);
                    long precisamAjuda = porSituacao.getOrDefault(Situacao.PRECISA_AJUDA, 0L);
                    long semContato = porSituacao.getOrDefault(Situacao.SEM_CONTATO, 0L);
                    long total = pendentes + precisamAjuda + semContato;
                    return new MapaDistritoDTO(distrito.getId(), distrito.getCodigo(), distrito.getNome(),
                            total, pendentes, precisamAjuda, semContato, cor(total));
                })
                .sorted(Comparator.comparingLong(MapaDistritoDTO::total).reversed()
                        .thenComparing(MapaDistritoDTO::nome))
                .toList();

        long totalGeral = distritos.stream().mapToLong(MapaDistritoDTO::total).sum();
        return new MapaResponseDTO(alerta.getId(), alerta.getTitulo(), alerta.getStatus(), totalGeral, distritos);
    }

    @Transactional(readOnly = true)
    public CorDistrito corDoDistrito(Long alertaId, Long distritoId) {
        return cor(situacaoRepository.countByAlertaIdAndEnderecoDistritoIdAndSituacaoIn(
                alertaId, distritoId, SEM_SEGURANCA));
    }

    /** Faixas configuráveis no application.properties (beaconsync.mapa.*). */
    public CorDistrito cor(long total) {
        if (total >= faixaVermelho) {
            return CorDistrito.VERMELHO;
        }
        if (total >= faixaLaranja) {
            return CorDistrito.LARANJA;
        }
        if (total >= faixaAmarelo) {
            return CorDistrito.AMARELO;
        }
        return CorDistrito.AZUL;
    }
}