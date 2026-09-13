package sptech.school.etiquetas.etiqueta.domain;

public interface GerarPdfEtiquetaGateway {

    byte[] gerar(Etiqueta etiqueta);
}
