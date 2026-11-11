package br.com.fiap.beaconsync.distrito;

public record DistritoResponseDTO(Long id, Integer codigo, String nome, String subprefeitura) {

    public DistritoResponseDTO(Distrito distrito) {
        this(distrito.getId(), distrito.getCodigo(), distrito.getNome(), distrito.getSubprefeitura());
    }
}