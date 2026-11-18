package br.com.fiap.beaconsync.alerta;

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