package br.com.fiap.beaconsync.resgate;

import br.com.fiap.beaconsync.alerta.MapaService;
import br.com.fiap.beaconsync.alerta.SituacaoNoAlerta;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PrioridadeService {

    private static final int GRAVIDADE_PADRAO = 3;

    private final MapaService mapaService;

    @Value("${beaconsync.prioridade.peso-gravidade}")
    private double pesoGravidade;

    @Value("${beaconsync.prioridade.peso-tempo}")
    private double pesoTempo;

    @Value("${beaconsync.prioridade.peso-cor}")
    private double pesoCor;

    @Value("${beaconsync.prioridade.minutos-tempo-maximo}")
    private double minutosTempoMaximo;

    public BigDecimal calcular(SituacaoNoAlerta situacao, LocalDateTime abertaEm) {
        int gravidade = situacao.getGravidade() != null ? situacao.getGravidade() : GRAVIDADE_PADRAO;

        long minutos = abertaEm != null ? Math.max(0, Duration.between(abertaEm, LocalDateTime.now()).toMinutes()) : 0;
        double fatorTempo = Math.min(minutos / minutosTempoMaximo, 1.0);

        double fatorCor = mapaService.corDoDistrito(
                situacao.getAlerta().getId(), situacao.getEndereco().getDistrito().getId()).getPeso();

        double score = pesoGravidade * gravidade / 5.0 + pesoTempo * fatorTempo + pesoCor * fatorCor;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }
}