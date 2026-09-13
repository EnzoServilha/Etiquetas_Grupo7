package sptech.school.etiquetas.etiqueta.domain;

public record Etiqueta(
        String codigoInterno,
        String marca,
        String descricao,
        String dataCadastro
) {
    public Etiqueta {
        codigoInterno = codigoInterno == null ? "" : codigoInterno;
        marca = marca == null ? "" : marca;
        descricao = descricao == null ? "" : descricao;
        dataCadastro = dataCadastro == null ? "" : dataCadastro;
    }
}
