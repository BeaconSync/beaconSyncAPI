package br.com.fiap.beaconsync.ia;

import java.util.List;

/**
 * Resultado da triagem de um pedido de ajuda. Na etapa 4, é este mesmo record
 * que o LLM devolve como saída estruturada (ChatClient .entity()).
 */
public record TriagemResultado(
        int gravidade,
        boolean riscoImediato,
        List<String> necessidades,
        String resumoParaEquipe
) {}