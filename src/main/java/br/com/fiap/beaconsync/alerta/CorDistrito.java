package br.com.fiap.beaconsync.alerta;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Cor do distrito no mapa. O peso entra no cálculo da prioridade sugerida. */
@Getter
@RequiredArgsConstructor
public enum CorDistrito {
    AZUL(0.0),
    AMARELO(0.33),
    LARANJA(0.66),
    VERMELHO(1.0);

    private final double peso;
}