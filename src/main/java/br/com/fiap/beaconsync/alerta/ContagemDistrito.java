package br.com.fiap.beaconsync.alerta;

/** Projeção da contagem do mapa: quantas pessoas em cada situação, por distrito. */
public interface ContagemDistrito {

    Long getDistritoId();

    Situacao getSituacao();

    Long getTotal();
}