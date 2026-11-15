package br.com.fiap.beaconsync.alerta;

/** Projeção simples: total por distrito. */
public interface ContagemPorDistrito {

    Long getDistritoId();

    Long getTotal();
}