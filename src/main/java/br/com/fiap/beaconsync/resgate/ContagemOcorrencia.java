package br.com.fiap.beaconsync.resgate;

/** Projeção do boletim: ocorrências por distrito e etapa. */
public interface ContagemOcorrencia {

    Long getDistritoId();

    StatusOcorrencia getStatus();

    Long getTotal();
}