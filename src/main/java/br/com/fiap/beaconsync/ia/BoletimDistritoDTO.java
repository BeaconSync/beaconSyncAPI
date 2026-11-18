package br.com.fiap.beaconsync.ia;

import br.com.fiap.beaconsync.alerta.CorDistrito;

public record BoletimDistritoDTO(
        Long distritoId,
        String nome,
        CorDistrito cor,
        long semSeguranca,
        long semResposta,
        long precisamAjuda,
        long semContato,
        long acamadosOuDependentes,
        long resgatesEmAndamento,
        long resgatesConcluidos
) {}