package br.com.fiap.beaconsync.ia;

import java.util.List;

public record TriagemResultado(
        int gravidade,
        boolean riscoImediato,
        List<String> necessidades,
        String resumoParaEquipe
) {}