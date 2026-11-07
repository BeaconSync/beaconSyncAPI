package br.com.fiap.beaconsync.distrito;

public record DistritoResponse(Long id, Integer codigo, String nome, String subprefeitura) {

    public static DistritoResponse de(Distrito distrito) {
        return new DistritoResponse(
                distrito.getId(),
                distrito.getCodigo(),
                distrito.getNome(),
                distrito.getSubprefeitura());
    }
}