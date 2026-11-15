package br.com.fiap.beaconsync.ia;

import br.com.fiap.beaconsync.alerta.CorDistrito;

/** Números brutos de um distrito, devolvidos junto com o texto para o agente conferir. */
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