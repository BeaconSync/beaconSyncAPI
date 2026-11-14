package br.com.fiap.beaconsync.alerta;

/**
 * Um distrito do mapa. total = pessoas que ainda não estão seguras
 * (pendentes + precisam de ajuda + sem contato). Não traz nomes.
 */
public record MapaDistritoDTO(
        Long distritoId,
        Integer codigo,
        String nome,
        long total,
        long pendentes,
        long precisamAjuda,
        long semContato,
        CorDistrito cor
) {}