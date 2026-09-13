package sptech.school.etiquetas.etiqueta.domain;

public interface BuscarEtiquetaGateway {

    Etiqueta buscarPorItemId(Integer itemId, String tokenAcesso);
}
