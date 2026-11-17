package br.com.fiap.beaconsync.ia.distrito;

import br.com.fiap.beaconsync.alerta.AlertaRepository;
import br.com.fiap.beaconsync.alerta.Situacao;
import br.com.fiap.beaconsync.alerta.SituacaoNoAlertaRepository;
import br.com.fiap.beaconsync.auth.AuthorizationService;
import br.com.fiap.beaconsync.exception.RegraNegocioException;
import br.com.fiap.beaconsync.usuario.Papel;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;

@Service
public class DadosDistritoService {
    private final AlertaRepository alertas;
    private final SituacaoNoAlertaRepository situacoes;
    private final AuthorizationService authorization;
    private final Clock clock;

    public DadosDistritoService(AlertaRepository alertas, SituacaoNoAlertaRepository situacoes,
                                AuthorizationService authorization,
                                @Qualifier("iaDistritoClock") Clock clock) {
        this.alertas = alertas;
        this.situacoes = situacoes;
        this.authorization = authorization;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public IaAnaliseRequest consultar(Long alertaId, Long distritoId, String pergunta) {
        Papel papel = authorization.usuarioLogado().getPapel();
        if (papel != Papel.AGENTE_ORGAO && papel != Papel.VOLUNTARIO) {
            throw new AccessDeniedException("Somente agentes e voluntários podem solicitar a análise agregada.");
        }
        var alerta = alertas.findById(alertaId)
                .orElseThrow(() -> new EntityNotFoundException("Alerta não encontrado."));
        var distrito = alerta.getDistritos().stream()
                .filter(d -> d.getId().equals(distritoId)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Distrito não pertence à área deste alerta."));

        var consultadoEm = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        var contagens = new EnumMap<Situacao, Long>(Situacao.class);
        for (var linha : situacoes.contarPorDistrito(alertaId, EnumSet.allOf(Situacao.class))) {
            if (distritoId.equals(linha.getDistritoId())) {
                contagens.put(linha.getSituacao(), linha.getTotal());
            }
        }
        if (contagens.values().stream().anyMatch(n -> n < 0 || n > 1_000_000)) {
            throw new RegraNegocioException("Contagem fora do limite aceito pelo serviço de análise.");
        }
        var totais = new IaAnaliseRequest.Situacoes(
                contagens.getOrDefault(Situacao.PRECISA_AJUDA, 0L),
                contagens.getOrDefault(Situacao.SEM_CONTATO, 0L),
                contagens.getOrDefault(Situacao.PENDENTE, 0L),
                contagens.getOrDefault(Situacao.SEGURA, 0L),
                contagens.getOrDefault(Situacao.RESGATADA, 0L));
        return new IaAnaliseRequest(alertaId.toString(), distritoId.toString(), distrito.getNome(),
                consultadoEm.toString(), totais, pergunta);
    }
}